import os, json
from typing import List, Optional, Any, Dict
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
import httpx

OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://ollama:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "phi3:mini")
OLLAMA_NUM_CTX = int(os.getenv("OLLAMA_NUM_CTX", "2048"))

api = FastAPI(title="AI Enricher API")

class VocabItem(BaseModel):
    term: str
    definition: str

class EnrichRequest(BaseModel):
    title: str = Field(..., description="APOD title")
    explanation: str = Field(..., description="APOD explanation text")
    grade: int = Field(4, description="Target grade level, e.g., 3..8")
    max_vocab: int = Field(3, ge=0, le=6)
    temperature: float = Field(0.6, ge=0.0, le=1.5)

class EnrichResponse(BaseModel):
    hook: str
    simple_explanation: str
    why_it_matters: str
    class_question: str
    vocabulary: List[VocabItem] = []
    fun_fact: str
    attribution: Optional[str] = None
    _meta: Dict[str, Any] = {}

@api.get("/health")
def health():
    return {"ok": True, "model": OLLAMA_MODEL}

def build_prompt(data: EnrichRequest) -> str:
    return f"""You are an educator assistant. Transform the NASA APOD text into concise slide content for grade {data.grade}.
Return ONLY minified JSON with keys:
hook, simple_explanation, why_it_matters, class_question, vocabulary (array of objects {{term,definition}}), fun_fact, attribution.

Constraints:
- hook: 1-2 short sentences.
- simple_explanation: 3-4 sentences, plain words for grade {data.grade}.
- why_it_matters: 1-2 sentences.
- class_question: exactly 1 question.
- vocabulary: up to {data.max_vocab} items, simple definitions.
- fun_fact: 1 sentence.
- attribution: 'NASA APOD' and photographer if provided.

APOD_TITLE: {data.title}
APOD_EXPLANATION: {data.explanation}

Output example:
{{"hook":"...","simple_explanation":"...","why_it_matters":"...","class_question":"...","vocabulary":[{{"term":"...","definition":"..."}},{{"term":"...","definition":"..."}}],"fun_fact":"...","attribution":"NASA APOD / <author if any>"}}"""

async def ollama_generate(prompt: str, temperature: float) -> str:
    payload = {
        "model": OLLAMA_MODEL,
        "prompt": prompt,
        "options": {"temperature": temperature, "num_ctx": OLLAMA_NUM_CTX},
        "stream": False
    }
    async with httpx.AsyncClient(timeout=120.0) as client:
        r = await client.post(f"{OLLAMA_BASE_URL}/api/generate", json=payload)
        if r.status_code != 200:
            raise HTTPException(r.status_code, f"Ollama error: {r.text}")
        return r.json().get("response","")

def extract_json(text: str) -> Dict[str, Any]:
    try:
        start = text.index("{")
        end = text.rindex("}") + 1
        return json.loads(text[start:end])
    except Exception:
        raise HTTPException(502, "LLM did not return valid JSON")

@api.post("/enrich", response_model=EnrichResponse)
async def enrich(body: EnrichRequest):
    prompt = build_prompt(body)
    raw = await ollama_generate(prompt, body.temperature)
    data = extract_json(raw)

    resp = EnrichResponse(**{
        "hook": data.get("hook",""),
        "simple_explanation": data.get("simple_explanation",""),
        "why_it_matters": data.get("why_it_matters",""),
        "class_question": data.get("class_question",""),
        "vocabulary": data.get("vocabulary",[]),
        "fun_fact": data.get("fun_fact",""),
        "attribution": data.get("attribution") or "NASA APOD",
        "_meta": {"model": OLLAMA_MODEL}
    })

    if not resp.hook or not resp.simple_explanation:
        raise HTTPException(502, "Incomplete response from LLM")
    return resp