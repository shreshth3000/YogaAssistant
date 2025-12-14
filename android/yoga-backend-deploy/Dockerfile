FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY recommendation_backend.py .
COPY yoga_embeddings.pkl .

ENV PORT=8080

CMD ["sh", "-c", "uvicorn recommendation_backend:app --host 0.0.0.0 --port ${PORT} --timeout-keep-alive 120"]
