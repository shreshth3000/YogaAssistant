FROM python:3.11-slim

WORKDIR /app

# Copy requirements
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy app and data
COPY recommendation_backend.py .
COPY yoga_embeddings.pkl .

# Expose port
EXPOSE 8080

# Run app
CMD ["uvicorn", "recommendation_backend:app", "--host", "0.0.0.0", "--port", "8080"]
