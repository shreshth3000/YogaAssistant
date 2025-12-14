from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
import pickle
import numpy as np
import os
import gc

from sentence_transformers import SentenceTransformer
import google.generativeai as genai
from dotenv import load_dotenv
from functools import lru_cache

# --------------------------------------------------
# Environment and app setup
# --------------------------------------------------

load_dotenv()

print("Starting Yoga Backend...")

app = FastAPI(title="Yoga Backend", version="1.2")

api_key = os.getenv("GOOGLE_API_KEY", "")
if api_key:
    genai.configure(api_key=api_key)
else:
    print("WARNING: GOOGLE_API_KEY not set")

# --------------------------------------------------
# Singleton model loader (prevents memory growth)
# --------------------------------------------------

@lru_cache(maxsize=1)
def get_model():
    return SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")

# --------------------------------------------------
# Load embeddings ONCE (no pandas at runtime)
# --------------------------------------------------

PKL_PATH = os.path.join(os.path.dirname(__file__), "yoga_embeddings.pkl")

with open(PKL_PATH, "rb") as f:
    df = pickle.load(f)

POSE_NAMES = df["AName"].tolist()
BENEFITS = df["Benefits"].tolist()
CONTRA = df["Contraindications"].fillna("").tolist()
BENEFITS_EMB = np.vstack(df["Benefits_emb"].values)

print(f"Loaded {len(POSE_NAMES)} yoga poses")

# --------------------------------------------------
# Request models
# --------------------------------------------------

class UserInput(BaseModel):
    age: int
    height: int
    weight: int
    goals: List[str]
    physical_issues: List[str]
    mental_issues: List[str]
    level: str

class ChatRequest(BaseModel):
    message: str

class ChatResponse(BaseModel):
    response: str

# --------------------------------------------------
# Recommendation logic (bounded + memory safe)
# --------------------------------------------------

def recommend_asanas(user_profile):
    model = get_model()

    query_text = " ".join(
        user_profile["goals"]
        + user_profile["physical_issues"]
        + user_profile["mental_issues"]
    )

    query_emb = model.encode(query_text, normalize_embeddings=True)
    sims = np.dot(BENEFITS_EMB, query_emb)

    top_idx = np.argsort(sims)[::-1][:10]

    results = []
    for i in top_idx:
        if sims[i] > 0:
            results.append({
                "name": POSE_NAMES[i],
                "score": round(float(sims[i]), 3),
                "benefits": BENEFITS[i],
                "contraindications": CONTRA[i]
            })

    del query_emb, sims
    gc.collect()

    return results

@app.post("/recommend/")
async def get_recommendations(user_input: UserInput):
    return {"recommended_asanas": recommend_asanas(user_input.dict())}

# --------------------------------------------------
# Conversational RAG architecture
# --------------------------------------------------

SYSTEM_PROMPT = """
You are a calm, professional yoga instructor and assistant.

Your role:
- Hold a natural, conversational dialogue.
- Explain yoga concepts clearly and accessibly.
- Use provided yoga knowledge when relevant.
- Answer general yoga questions even if no poses are referenced.
- Prioritize safety and clarity.

Rules:
- Mention contraindications when poses or injuries are discussed.
- If the knowledge section is empty or irrelevant, answer from general yoga understanding.
- Do not provide medical diagnoses.
- Keep responses concise and helpful (3–6 sentences).
"""

def retrieve_context(query: str, k: int = 5) -> str:
    model = get_model()

    query_emb = model.encode(query, normalize_embeddings=True)
    sims = np.dot(BENEFITS_EMB, query_emb)

    top_idx = np.argsort(sims)[::-1][:k]

    context_blocks = []
    for i in top_idx:
        if sims[i] > 0.15:
            context_blocks.append(
                f"Pose: {POSE_NAMES[i]}\n"
                f"Benefits: {BENEFITS[i]}\n"
                f"Contraindications: {CONTRA[i]}"
            )

    del query_emb, sims
    return "\n\n".join(context_blocks)

@app.post("/chat/", response_model=ChatResponse)
async def chat(request: ChatRequest):
    try:
        query = request.message.strip()
        context = retrieve_context(query)

        prompt = f"""
{SYSTEM_PROMPT}

Yoga Knowledge (may be empty):
{context if context else "No specific pose data retrieved."}

User: {query}
Instructor:
"""

        response = genai.GenerativeModel(
            "gemini-2.0-flash"
        ).generate_content(prompt)

        gc.collect()

        return ChatResponse(response=response.text)

    except Exception as e:
        print(f"Chat error: {e}")
        return ChatResponse(
            response="I ran into an issue while answering. Please try again."
        )

# --------------------------------------------------
# Health endpoint
# --------------------------------------------------

@app.get("/health")
async def health():
    return {
        "status": "ok",
        "poses_loaded": len(POSE_NAMES),
        "model_loaded": True
    }
