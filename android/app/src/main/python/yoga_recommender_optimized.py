import pandas as pd
import numpy as np
import pickle
import os
import json
from typing import Dict, List, Optional

# Global model cache
_model_cache = None
_embeddings_cache = None

class OptimizedYogaRecommender:
    def __init__(self, embeddings_path: str):
        """
        Initialize the yoga recommender with pre-computed embeddings
        Uses a much smaller approach without sentence transformers
        """
        self.embeddings_path = embeddings_path
        self.df = None
        self.load_embeddings(embeddings_path)
        
    def load_embeddings(self, embeddings_path: str):
        """Load the pre-computed embeddings from pickle file"""
        global _embeddings_cache
        
        try:
            if _embeddings_cache is None:
                with open(embeddings_path, 'rb') as f:
                    _embeddings_cache = pickle.load(f)
                print(f"Loaded {len(_embeddings_cache)} yoga poses with embeddings")
            
            self.df = _embeddings_cache
            
        except Exception as e:
            print(f"Error loading embeddings: {e}")
            self.df = None
    
    def simple_text_similarity(self, text1: str, text2: str) -> float:
        """
        Simple text similarity using word overlap
        Much faster than sentence transformers
        """
        if not text1 or not text2:
            return 0.0
            
        words1 = set(text1.lower().split())
        words2 = set(text2.lower().split())
        
        if not words1 or not words2:
            return 0.0
            
        intersection = words1.intersection(words2)
        union = words1.union(words2)
        
        return len(intersection) / len(union) if union else 0.0
    
    def get_recommendations(self, user_profile: Dict) -> List[Dict]:
        """
        Get yoga pose recommendations based on user profile
        Uses simple text similarity instead of sentence transformers
        """
        if self.df is None:
            return []
        
        recommendations = []
        weights = {
            "goals_benefits": 4,
            "physical_benefits": 4,
            "mental_benefits": 4,
            "physical_match": 2,
            "mental_match": 2,
        }
        total_weight = sum(weights.values())
        
        # Prepare user input text
        user_goals_text = " ".join(user_profile.get("goals", []))
        user_physical_text = " ".join(user_profile.get("physical_issues", []))
        user_mental_text = " ".join(user_profile.get("mental_issues", []))
        
        for i, row in self.df.iterrows():
            score = 0.0
            contra_text = str(row["Contraindications"]).lower()
            
            # Check contraindications using simple text matching
            discard = False
            for issue in user_profile.get("physical_issues", []) + user_profile.get("mental_issues", []):
                issue = issue.lower()
                
                # Literal match
                if issue in contra_text:
                    discard = True
                    break
                
                # Simple similarity check
                if self.simple_text_similarity(issue, contra_text) > 0.3:
                    discard = True
                    break
            
            if discard:
                continue
            
            # Calculate similarities using simple text matching
            goals_similarity = self.simple_text_similarity(user_goals_text, str(row["Benefits"]))
            physical_similarity = self.simple_text_similarity(user_physical_text, str(row["Benefits"]))
            mental_similarity = self.simple_text_similarity(user_mental_text, str(row["Benefits"]))
            
            physical_match_similarity = self.simple_text_similarity(user_physical_text, str(row["Targeted Physical Problems"]))
            mental_match_similarity = self.simple_text_similarity(user_mental_text, str(row["Targeted Mental Problems"]))
            
            # Main positive contributions
            score += weights["goals_benefits"] * goals_similarity
            score += weights["physical_benefits"] * physical_similarity
            score += weights["mental_benefits"] * mental_similarity
            score += weights["physical_match"] * physical_match_similarity
            score += weights["mental_match"] * mental_match_similarity
            
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
        return recommendations[:10]  # Return top 10 recommendations

def get_recommendations_for_user(user_profile_json: str, embeddings_path: str) -> str:
    """
    Main function to get recommendations - called from Kotlin
    Uses optimized approach without sentence transformers
    """
    try:
        # Parse user profile
        user_profile = json.loads(user_profile_json)
        
        # Initialize recommender
        recommender = OptimizedYogaRecommender(embeddings_path)
        
        # Get recommendations
        recommendations = recommender.get_recommendations(user_profile)
        
        # Return as JSON
        return json.dumps(recommendations)
        
    except Exception as e:
        print(f"Error in get_recommendations_for_user: {e}")
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
