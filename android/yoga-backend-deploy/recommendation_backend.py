from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Optional
import pickle
import numpy as np
from sentence_transformers import SentenceTransformer, util
import google.generativeai as genai
import os
from dotenv import load_dotenv

load_dotenv()

app = FastAPI(title="Yoga Backend", version="1.0")

# Initialize Generative AI
genai.configure(api_key=os.getenv("GOOGLE_API_KEY", ""))

# Load embeddings
model = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")

# Try multiple paths for yoga_embeddings.pkl
import os
pkl_paths = [
    "yoga_embeddings.pkl",
    "/app/yoga_embeddings.pkl",
    os.path.join(os.path.dirname(__file__), "yoga_embeddings.pkl")
]

df = None
for path in pkl_paths:
    try:
        if os.path.exists(path):
            with open(path, "rb") as f:
                df = pickle.load(f)
            print(f"✓ Loaded yoga embeddings from {path}: {df.shape[0]} poses")
            print(f"✓ Columns: {df.columns.tolist()}")
            break
    except Exception as e:
        print(f"Could not load from {path}: {e}")

if df is None:
    print("WARNING: Could not load yoga_embeddings.pkl from any path")
    print(f"Current working directory: {os.getcwd()}")
    print(f"Files in current directory: {os.listdir('.')}")

class UserInput(BaseModel):
    age: int
    height: int
    weight: int
    goals: List[str]
    physical_issues: List[str]
    mental_issues: List[str]
    level: str

def recommend_asanas(user_profile):
    user_emb = {
        "goals": model.encode(" ".join(user_profile["goals"]), normalize_embeddings=True),
        "physical_issues": model.encode(" ".join(user_profile["physical_issues"]), normalize_embeddings=True),
        "mental_issues": model.encode(" ".join(user_profile["mental_issues"]), normalize_embeddings=True),
    }

    recommendations = []
    weights = {
        "goals_benefits": 4,
        "physical_benefits": 4,
        "mental_benefits": 4,
        "physical_match": 2,
        "mental_match": 2,
    }
    total_weight = sum(weights.values())

    for _, row in df.iterrows():
        score = 0.0
        contra_text = str(row["Contraindications"]).lower()

        discard = False
        for issue in user_profile["physical_issues"] + user_profile["mental_issues"]:
            issue = issue.lower()
            if issue in contra_text:
                discard = True
                break
            if util.cos_sim(model.encode(issue, normalize_embeddings=True), row["Contraindications_emb"]).item() > 0.25:
                discard = True
                break
        if discard:
            continue

        score += weights["goals_benefits"] * util.cos_sim(user_emb["goals"], row["Benefits_emb"]).item()
        score += weights["physical_benefits"] * util.cos_sim(user_emb["physical_issues"], row["Benefits_emb"]).item()
        score += weights["mental_benefits"] * util.cos_sim(user_emb["mental_issues"], row["Benefits_emb"]).item()

        score += weights["physical_match"] * util.cos_sim(user_emb["physical_issues"], row["Targeted Physical Problems_emb"]).item()
        score += weights["mental_match"] * util.cos_sim(user_emb["mental_issues"], row["Targeted Mental Problems_emb"]).item()

        score /= total_weight

        if score > 0:
            recommendations.append({
                "name": row["AName"],
                "score": round(score, 3),
                "benefits": row["Benefits"],
                "contraindications": row["Contraindications"]
            })

    recommendations = sorted(recommendations, key=lambda x: x["score"], reverse=True)
    return recommendations[:10]

@app.post("/recommend/")
async def get_recommendations(user_input: UserInput):
    user_profile = user_input.dict()
    recommended_asanas = recommend_asanas(user_profile)
    return {"recommended_asanas": recommended_asanas}


# ============ RAG CHATBOT ENDPOINT ============

class ChatRequest(BaseModel):
    message: str

class ChatResponse(BaseModel):
    response: str

@app.post("/chat/")
async def chat(request: ChatRequest):
    """RAG-based yoga chatbot using yoga_embeddings.pkl"""
    try:
        # 1. ENCODE QUERY
        query_embedding = model.encode(request.message, normalize_embeddings=True)
        
        # 2. SEARCH YOGA EMBEDDINGS FOR SIMILAR POSES
        similarities = []
        for idx, row in df.iterrows():
            # Search using Benefits embedding for relevance
            sim_score = float(np.dot(query_embedding, row["Benefits_emb"]))
            pose_info = {
                "name": row["AName"],
                "benefits": row["Benefits"],
                "contraindications": row.get("Contraindications", "None"),
                "score": sim_score
            }
            similarities.append(pose_info)
        
        # Get top 5 most relevant poses
        top_poses = sorted(similarities, key=lambda x: x["score"], reverse=True)[:5]
        
        if not top_poses or top_poses[0]["score"] < 0.1:
            return ChatResponse(response="I couldn't find relevant yoga information for that question. Try asking about specific poses or yoga benefits.")
        
        # 3. BUILD CONTEXT FROM RETRIEVED POSES
        context = "Here are relevant yoga poses from the knowledge base:\n\n"
        for pose in top_poses:
            context += f"- {pose['name']}\n"
            context += f"  Benefits: {pose['benefits']}\n"
            context += f"  Contraindications: {pose['contraindications']}\n\n"
        
        # 4. BUILD SYSTEM PROMPT
        system_prompt = """You are a professional yoga instructor with deep knowledge of yoga poses and their benefits.
Answer questions based on the provided yoga knowledge base.
Be specific, practical, and always prioritize safety.
Always mention contraindications when relevant.
Keep responses concise (2-3 sentences typically)."""
        
        # 5. GENERATE RESPONSE WITH GEMINI
        full_prompt = f"""{system_prompt}

KNOWLEDGE BASE:
{context}

User Question: {request.message}

Provide a helpful answer based on the knowledge base."""
        
        genai_model = genai.GenerativeModel("gemini-2.0-flash")
        response = genai_model.generate_content(full_prompt)
        
        return ChatResponse(response=response.text or "Unable to generate response. Please try again.")
        
    except Exception as e:
        print(f"Chat error: {str(e)}")
        return ChatResponse(response=f"Sorry, an error occurred: {str(e)}")

@app.get("/health")
async def health():
    return {
        "status": "ok",
        "service": "yoga-backend",
        "embeddings_loaded": df is not None,
        "api_key_configured": bool(os.getenv("GOOGLE_API_KEY"))
    }

