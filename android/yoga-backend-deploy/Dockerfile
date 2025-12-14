FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY recommendation_backend.py .
COPY yoga_embeddings.pkl .

EXPOSE 8080

ENV PORT=8080
CMD ["uvicorn", "recommendation_backend:app", "--host", "0.0.0.0", "--port", "8080", "--timeout-keep-alive", "120"]