import numpy as np
import pandas as pd
from sentence_transformers import SentenceTransformer, util
import pickle
import os
import json

class YogaRecommender:
    def __init__(self, embeddings_path):
        """
        Initialize the yoga recommender with pre-computed embeddings
        Uses the same approach as the notebook with sentence transformers
        """
        print("Loading sentence transformer model...")
        self.model = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")
        print("Sentence transformer model loaded successfully!")
        
        self.df = None
        self.load_embeddings(embeddings_path)
        
    def load_embeddings(self, embeddings_path):
        """Load the pre-computed embeddings from pickle file"""
        try:
            with open(embeddings_path, 'rb') as f:
                self.df = pickle.load(f)
            print(f"Loaded {len(self.df)} yoga poses with embeddings")
        except Exception as e:
            print(f"Error loading embeddings: {e}")
            self.df = None
    
    def get_recommendations(self, user_profile):
        """
        Get yoga pose recommendations based on user profile
        Uses the exact same logic as the notebook
        """
        if self.df is None:
            print("No embeddings loaded, returning empty recommendations")
            return []
        
        # Encode user input exactly like in the notebook
        user_emb = {
            "goals": self.model.encode(" ".join(user_profile.get("goals", [])), normalize_embeddings=True),
            "physical_issues": self.model.encode(" ".join(user_profile.get("physical_issues", [])), normalize_embeddings=True),
            "mental_issues": self.model.encode(" ".join(user_profile.get("mental_issues", [])), normalize_embeddings=True),
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
        
        print(f"Processing {len(self.df)} yoga poses for recommendations...")
        
        for i, row in self.df.iterrows():
            score = 0.0
            contra_text = str(row["Contraindications"]).lower()
            
            # Check contraindications exactly like in notebook
            discard = False
            for issue in user_profile.get("physical_issues", []) + user_profile.get("mental_issues", []):
                issue = issue.lower()
                
                # Literal match
                if issue in contra_text:
                    discard = True
                    break
                
                # Embedding similarity (LOW threshold = aggressive filtering)
                issue_emb = self.model.encode(issue, normalize_embeddings=True)
                similarity = util.cos_sim(issue_emb, row["Contraindications_emb"]).item()
                if similarity > 0.25:
                    discard = True
                    break
            
            if discard:
                continue
            
            # Main positive contributions - exactly like notebook
            score += weights["goals_benefits"] * util.cos_sim(user_emb["goals"], row["Benefits_emb"]).item()
            score += weights["physical_benefits"] * util.cos_sim(user_emb["physical_issues"], row["Benefits_emb"]).item()
            score += weights["mental_benefits"] * util.cos_sim(user_emb["mental_issues"], row["Benefits_emb"]).item()
            
            score += weights["physical_match"] * util.cos_sim(user_emb["physical_issues"], row["Targeted Physical Problems_emb"]).item()
            score += weights["mental_match"] * util.cos_sim(user_emb["mental_issues"], row["Targeted Mental Problems_emb"]).item()
            
            # Normalize
            score /= total_weight
            
            if score > 0:
                recommendations.append({
                    "name": row["AName"],
                    "score": round(score, 3),
                    "benefits": row["Benefits"],
                    "contraindications": row["Contraindications"],
                    "level": row.get("Level", "Beginner"),
                    "description": row.get("Description", "")
                })
        
        # Sort by descending score and return top recommendations
        recommendations = sorted(recommendations, key=lambda x: x["score"], reverse=True)
        print(f"Generated {len(recommendations)} recommendations")
        return recommendations[:10]  # Return top 10 recommendations

def get_recommendations_for_user(user_profile_json, embeddings_path):
    """
    Main function to get recommendations - called from Kotlin
    Uses the exact same approach as the notebook
    """
    try:
        print("Starting recommendation generation...")
        
        # Parse user profile
        user_profile = json.loads(user_profile_json)
        print(f"User profile: {user_profile}")
        
        # Initialize recommender
        recommender = YogaRecommender(embeddings_path)
        
        # Get recommendations
        recommendations = recommender.get_recommendations(user_profile)
        
        print(f"Recommendations generated: {len(recommendations)}")
        
        # Return as JSON
        return json.dumps(recommendations)
        
    except Exception as e:
        print(f"Error in get_recommendations_for_user: {e}")
        import traceback
        traceback.print_exc()
        return json.dumps([])

# Test function for debugging
if __name__ == "__main__":
    # Test with sample user profile
    test_profile = {
        "age": 25,
        "height": 170,
        "weight": 65,
        "goals": ["flexibility"],
        "physical_issues": ["back pain"],
        "mental_issues": ["anxiety"],
        "level": "beginner",
        "pregnant": False
    }
    
    # Assuming embeddings file is in the same directory
    embeddings_path = "yoga_embeddings.pkl"
    
    if os.path.exists(embeddings_path):
        result = get_recommendations_for_user(json.dumps(test_profile), embeddings_path)
        print("Test recommendations:")
        print(result)
    else:
        print(f"Embeddings file not found at {embeddings_path}")
