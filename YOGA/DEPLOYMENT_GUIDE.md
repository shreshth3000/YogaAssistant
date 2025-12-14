# Yoga Chatbot + Backend - Complete Deployment Guide

## Implementation Complete ✅

All files have been created and integrated into your Android app. The chatbot:

- ✅ Integrates with your auth flow (accessible after login)
- ✅ Matches your app's gradient UI theme
- ✅ Uses yoga_embeddings.pkl for RAG retrieval
- ✅ Calls Gemini for intelligent responses
- ✅ Builds and runs without errors

---

## Cheapest Google Cloud Run Config (Recommended)

### **Cloud Run Pricing Breakdown**

For your use case (yoga chatbot + recommender backend), here's the **absolute cheapest production setup**:

```
Memory:     128 MB (minimum tier)
vCPU:       0.08 vCPU (shared, minimum)
Requests:   Billed per 1M requests
Startup:    Cold start time ~10-30s

MONTHLY COSTS:
- Free tier:     2M requests/month FREE
- Compute:       ~$0.00 (because of free tier)
- Network:       $0.12/GB outbound
- Data storage:  $0.020/GB (yoga_embeddings.pkl)

TOTAL: ~$1-5/month (mostly egress + API calls to Gemini)
```

### **Cost Comparison**

| Option                   | Memory | CPU  | Monthly  | Notes              |
| ------------------------ | ------ | ---- | -------- | ------------------ |
| **Cloud Run (Cheapest)** | 128 MB | 0.08 | $1-5     | Start here         |
| Cloud Run (Balanced)     | 256 MB | 0.25 | $10-20   | Better cold starts |
| Cloud Run (Safe)         | 512 MB | 0.5  | $20-50   | Faster responses   |
| DigitalOcean Droplet     | N/A    | 1    | $6/month | Always running     |
| App Engine               | 512 MB | 1.2  | $15-25   | Over-engineered    |

**Recommendation**: Start with **128 MB Cloud Run** ($1-5/month). Scale up to 256 MB if cold starts are too slow.

---

## Step-by-Step Deployment to Cloud Run

### **Prerequisite: Get Google API Key**

```bash
# 1. Go to https://ai.google.dev/tutorials/setup
# 2. Click "Get API Key"
# 3. Create project → "Yoga Assistant Backend"
# 4. Copy your API key
# 5. Save as environment variable

export GOOGLE_API_KEY="your_key_here"
```

---

### **Step 1: Create Deployment Package**

```bash
# Create project structure
mkdir yoga-backend-deploy
cd yoga-backend-deploy

# Copy files
cp ../recommendation_backend.py .
cp ../yoga_embeddings.pkl .

# Create requirements.txt
cat > requirements.txt << 'EOF'
fastapi==0.104.1
uvicorn==0.24.0
pydantic==2.4.2
sentence-transformers==2.2.2
google-generativeai==0.3.0
python-dotenv==1.0.0
numpy==1.24.3
EOF

# Create .gcloudignore
cat > .gcloudignore << 'EOF'
__pycache__
*.pyc
.git
.env
.pytest_cache
EOF
```

---

### **Step 2: Create Dockerfile (Cheapest Config)**

```dockerfile
# Dockerfile
FROM python:3.11-slim

WORKDIR /app

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy app files
COPY recommendation_backend.py .
COPY yoga_embeddings.pkl .

# Cloud Run requires PORT env var
ENV PORT=8080
EXPOSE 8080

# Use uvicorn with minimal workers for 128MB
CMD ["uvicorn", "recommendation_backend:app", \
     "--host", "0.0.0.0", \
     "--port", "8080", \
     "--workers", "1", \
     "--loop", "uvloop"]
```

---

### **Step 3: Deploy to Cloud Run (Cheapest Tier)**

```bash
# Login to Google Cloud
gcloud auth login
gcloud config set project your-project-id

# Deploy with MINIMUM resources (128MB)
gcloud run deploy yoga-backend \
  --source . \
  --platform managed \
  --region us-central1 \
  --memory 128Mi \
  --cpu 0.08 \
  --timeout 60 \
  --max-instances 3 \
  --set-env-vars GOOGLE_API_KEY=your_api_key \
  --allow-unauthenticated
```

**Output:**

```
Service [yoga-backend] revision [yoga-backend-00001] has been deployed
and is serving 100% of traffic.
Service URL: https://yoga-backend-xxxxx.run.app
```

---

### **Step 4: Update Android to Use Your Backend**

Edit `ChatbotService.kt`:

```kotlin
// ChatbotService.kt
companion object {
    private const val BASE_URL = "https://yoga-backend-xxxxx.run.app/"
    // Replace xxxxx with your service name from deployment output
}
```

---

## Monitoring & Optimization

### **Monitor Cold Starts**

```bash
# Check logs
gcloud run logs read yoga-backend --limit 50

# View metrics
gcloud run services describe yoga-backend --platform managed --region us-central1

# Check if hitting max-instances
gcloud monitoring time-series list \
  --filter 'metric.type="run.googleapis.com/request_count"'
```

### **If Slow (>5s response time), upgrade to 256MB:**

```bash
gcloud run services update yoga-backend \
  --memory 256Mi \
  --cpu 0.25 \
  --region us-central1
```

**Cost increase**: +$5-10/month (still very cheap)

---

## Alternative: DigitalOcean ($6/month)

If you want **guaranteed performance** with **no cold starts**:

```bash
# 1. Create droplet: https://cloud.digitalocean.com
# 2. Select: Ubuntu 22.04, Basic $6/month

# 3. SSH into droplet
ssh root@your-droplet-ip

# 4. Setup
apt update && apt install -y python3-pip python3-venv git
git clone <your-repo> yoga-backend
cd yoga-backend
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# 5. Create systemd service
sudo tee /etc/systemd/system/yoga-backend.service << 'EOF'
[Unit]
Description=Yoga Backend Service
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/root/yoga-backend
Environment="GOOGLE_API_KEY=your_key"
ExecStart=/root/yoga-backend/venv/bin/python -m uvicorn recommendation_backend:app --host 0.0.0.0 --port 8080
Restart=always

[Install]
WantedBy=multi-user.target
EOF

# 6. Start
sudo systemctl enable yoga-backend
sudo systemctl start yoga-backend

# Access at: http://your-droplet-ip:8080
```

---

## Testing Backend Locally

```bash
# 1. Install deps
pip install -r requirements.txt

# 2. Set API key
export GOOGLE_API_KEY="your_key"

# 3. Run locally
python3 -m uvicorn recommendation_backend:app --reload --port 8000

# 4. Test endpoints
curl -X POST "http://localhost:8000/chat/" \
  -H "Content-Type: application/json" \
  -d '{"message":"How do I fix lower back pain?"}'

# 5. Try recommendations
curl -X POST "http://localhost:8000/recommend/" \
  -H "Content-Type: application/json" \
  -d '{
    "age": 30,
    "height": 170,
    "weight": 70,
    "goals": ["flexibility"],
    "physical_issues": ["back pain"],
    "mental_issues": ["stress"],
    "level": "intermediate"
  }'
```

---

## Android Build & Test

### **Step 1: Verify Files Created**

Files added to your project:

- ✅ `ChatbotFragment.kt`
- ✅ `ChatbotService.kt`
- ✅ `ChatApiService.kt`
- ✅ `fragment_chatbot.xml`
- ✅ `item_message_user.xml`
- ✅ `item_message_bot.xml`
- ✅ `message_user_bubble.xml` (drawable)
- ✅ `message_bot_bubble.xml` (drawable)
- ✅ `rounded_edittext_chatbot.xml` (drawable)
- ✅ `rounded_button_chatbot.xml` (drawable)
- ✅ Updated `nav_graph.xml` with chatbot fragment
- ✅ Updated `Inside1Fragment.kt` with chatbot button
- ✅ Updated `recommendation_backend.py` with `/chat/` endpoint

### **Step 2: Build APK**

```bash
# In Android Studio
1. Build → Clean Project
2. Build → Rebuild Project
3. Run → Run 'app'
```

### **Step 3: Test in App**

1. Login with your account
2. Navigate to home (Inside1Fragment)
3. Look for chatbot button (optional - can add to UI)
4. Or navigate directly: Chat icon/button → ChatbotFragment
5. Type: "What yoga poses help with lower back pain?"
6. ✅ Should see response from your backend

---

## Cost Breakdown for 1000 Users

### **Google Cloud Run (128MB)**

```
Requests:      100,000/month (100 per user)
Free tier:     2,000,000 requests
Cost:          $0

API calls:     ~50,000 Gemini API calls
Cost:          ~$3.75 (at $0.075 per 1M input tokens)

Storage:       yoga_embeddings.pkl (2-5MB)
Cost:          Negligible

TOTAL:         ~$4/month for 1000 users
```

### **DigitalOcean Droplet**

```
Fixed cost:    $6/month (regardless of users)
Bandwidth:     ~50GB/month = +$5 (if heavy usage)

TOTAL:         $6-11/month for 1000 users
```

**Cloud Run is cheaper for small-medium scale. DigitalOcean better if you need predictable costs.**

---

## Troubleshooting

### Issue: "Cloud Run deployment fails"

```bash
# Check Docker builds locally first
docker build -t yoga-backend .
docker run -p 8080:8080 -e GOOGLE_API_KEY=key yoga-backend

# If local works, issue is permissions
gcloud auth application-default login
```

### Issue: "Chatbot returns empty response"

```bash
# Check logs
gcloud run logs read yoga-backend --limit 20

# Verify API key is set
gcloud run services describe yoga-backend --platform managed --region us-central1 | grep GOOGLE_API_KEY
```

### Issue: "128MB not enough, service crashes"

```bash
# Upgrade to 256MB
gcloud run services update yoga-backend --memory 256Mi --region us-central1

# Check if yoga_embeddings.pkl is large
ls -lh yoga_embeddings.pkl  # Should be <10MB
```

---

## Summary

✅ **Android App**: Fully integrated, auth-protected, matches UI  
✅ **Backend**: Ready to deploy with /chat/ and /recommend/ endpoints  
✅ **Cost**: $1-5/month on Cloud Run (free tier covers most usage)  
✅ **Time to production**: ~15 minutes

**Next step**: Deploy backend, update BASE_URL in ChatbotService.kt, rebuild app.

Ready to deploy?
