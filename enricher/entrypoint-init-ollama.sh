#!/usr/bin/env sh
set -eu

: "${OLLAMA_BASE_URL:=http://ollama:11434}"
: "${OLLAMA_MODEL:=phi3:mini}"
WAIT="${OLLAMA_WAIT_TIMEOUT:-180}"

echo "[enricher-init] Waiting for Ollama at ${OLLAMA_BASE_URL} (timeout ${WAIT}s)..."
end=$(( $(date +%s) + WAIT ))
until curl -fsS "${OLLAMA_BASE_URL}/api/tags" >/dev/null 2>&1; do
  if [ "$(date +%s)" -ge "$end" ]; then
    echo "[enricher-init] Timeout waiting for Ollama" >&2
    exit 1
  fi
  sleep 1
done

echo "[enricher-init] Checking if model '${OLLAMA_MODEL}' is present..."
if curl -fsS "${OLLAMA_BASE_URL}/api/tags" | grep -q "\"name\":\"${OLLAMA_MODEL}\""; then
  echo "[enricher-init] Model already present."
else
  echo "[enricher-init] Pulling model '${OLLAMA_MODEL}'..."
  curl -fsS -X POST "${OLLAMA_BASE_URL}/api/pull" \
       -H "Content-Type: application/json" \
       -d "{\"name\":\"${OLLAMA_MODEL}\"}" \
    || { echo "[enricher-init] Pull failed" >&2; exit 1; }
  echo "[enricher-init] Model pulled."
fi

# warm up
if [ "${OLLAMA_WARMUP:-false}" = "true" ]; then
 curl -fsS -X POST "${OLLAMA_BASE_URL}/api/generate" \
   -H "Content-Type: application/json" \
   -d "{\"model\":\"${OLLAMA_MODEL}\",\"prompt\":\"ping\",\"stream\":false}" >/dev/null || true
fi

echo "[enricher-init] Starting app: $*"
exec "$@"