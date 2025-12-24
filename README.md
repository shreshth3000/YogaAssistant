# YogaAssistant

A comprehensive yoga assistance application with real-time pose detection, personalized recommendations, and AI-powered chatbot guidance.

## Features

- Real-time pose detection using Google ML Kit
- Personalized yoga recommendations based on user profile
- Pose calibration with joint angle feedback
- AI-powered conversational yoga assistant
- Multi-camera support (front and back)
- RAG-based chatbot using SentenceTransformers and Gemini 2.0 Flash

## Architecture

### Android Application

- Kotlin-based mobile app with CameraX for real-time video processing
- Google ML Kit for 33-point pose detection
- Local pose calibration and feedback system

### Backend Services

- FastAPI-based microservice running on Google Cloud Run
- Two main endpoints:
  - `/recommend/` - Personalized yoga recommendations
  - `/chat/` - Conversational yoga assistant with RAG
- If you make changes to the backend or the Cloud Run integration, you will have to ask @shreshth3000 to manually redeploy the backend.

### Machine Learning

- SentenceTransformers (all-MiniLM-L6-v2) for semantic search
- Pre-computed embeddings in yoga_embeddings.pkl
- Gemini 2.0 Flash for conversational AI

## Project Structure

```
YogaAssistant/
├── android/                    Android application
├── backend/
│   ├── recommender/            Recommendation engine
│   └── deployment/             Cloud Run deployment
├── data/                       Datasets and embeddings
├── models/                     ML models and training
├── notebooks/                  Jupyter notebooks
├── scripts/                    Utility scripts
└── assets/                     Generated icons and assets
```

## Quick Start

### Prerequisites

- Android Studio 2024.1+
- Python 3.11+
- Google Cloud account with billing enabled
- Google API key for Gemini

### Android Setup

1. Clone the repository
2. Open `android/` in Android Studio
3. Build and run on device or emulator

### Backend Setup

1. Navigate to `backend/deployment/`
2. Create `.env` file with `GOOGLE_API_KEY`
3. Run locally:
   ```
   python -m uvicorn recommendation_backend:app --reload --port 8000
   ```

### Deployment

Backend is deployed on Google Cloud Run with 2GiB memory allocation.

Deploy via Google Cloud Console:

1. Connect GitHub repository
2. Configure build settings (Dockerfile location: `backend/deployment/Dockerfile`)
3. Set environment variable: `GOOGLE_API_KEY`
4. Deploy to us-central1 region

## Key Components

### Pose Detection

- Detects 33 body landmarks in real-time
- Calculates 8 joint angles (shoulders, elbows, hips, knees)
- Provides visual feedback (green for correct, red for incorrect)

### Recommendation System

- Analyzes user profile (age, height, weight, fitness level, goals, issues)
- Filters poses based on contraindications
- Scores poses using multi-factor weighting
- Returns top 10 personalized recommendations

### Chatbot System

- Retrieves relevant poses from embeddings using semantic search
- Generates contextual responses using Gemini 2.0 Flash
- Maintains conversation history
- Auth-protected access

## API Endpoints

### POST /recommend/

Request:

```json
{
  "age": 30,
  "height": 170,
  "weight": 70,
  "goals": ["flexibility", "strength"],
  "physical_issues": ["back_pain"],
  "mental_issues": ["stress"],
  "level": "beginner"
}
```

Response:

```json
{
  "recommended_asanas": [
    {
      "name": "Downward Dog",
      "score": 0.85,
      "benefits": "...",
      "contraindications": "..."
    }
  ]
}
```

### POST /chat/

Request:

```json
{
  "message": "What yoga poses help with back pain?"
}
```

Response:

```json
{
  "response": "Based on the yoga knowledge base, several poses can help with back pain..."
}
```

### GET /health

Endpoint for monitoring service health.

## Data Files

- `yoga_embeddings.pkl` - Pre-computed embeddings (~2-5MB)
- `yoga_poses.json` - Reference pose angles and deviations
- `models/tflite/` - TensorFlow Lite models for on-device processing

## Configuration

### Environment Variables

- `GOOGLE_API_KEY` - Google API key for Gemini

### Android Configuration

- Base URL for backend in `ChatbotService.kt` and `NetworkService.kt`
- Defaults to `https://yoga-backend-xxxxx.run.app/`

## Performance

- Recommender: 100-200ms response time
- Chatbot cold start: 10-30 seconds (first request)
- Chatbot warm request: 2-3 seconds
- Monthly cost: 3-6 USD on Cloud Run (2GiB instance)

## Contributing

See CONTRIBUTING.md for guidelines on feature development, branching strategy, and pull request process.

## License

Proprietary - All rights reserved

## Support

For issues and feature requests, please use the GitHub Issues page.


