FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates curl && \
    rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install -r requirements.txt

COPY app.py .

COPY entrypoint-init-ollama.sh /usr/local/bin/entrypoint-init-ollama.sh
RUN chmod +x /usr/local/bin/entrypoint-init-ollama.sh

EXPOSE 8090
ENTRYPOINT ["/usr/local/bin/entrypoint-init-ollama.sh"]
CMD ["uvicorn", "app:api", "--host", "0.0.0.0", "--port", "8090"]