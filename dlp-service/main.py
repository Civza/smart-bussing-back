import os
from typing import List, Optional
from fastapi import FastAPI
from pydantic import BaseModel
from better_profanity import profanity

try:
    import datafog
    DATAFOG_AVAILABLE = True
except ImportError:
    DATAFOG_AVAILABLE = False

app = FastAPI(
    title="Smart Bussing DLP & Content Moderation Service",
    description="Microservicio de prevención de fuga de datos (DLP con DataFog Core) y filtro de lenguaje ofensivo para comentarios de rutas.",
    version="1.0.0"
)

# Cargar palabras no permitidas
WORDS_FILE = os.path.join(os.path.dirname(__file__), "banned_words.txt")
if os.path.exists(WORDS_FILE):
    with open(WORDS_FILE, "r", encoding="utf-8") as f:
        custom_words = [line.strip().lower() for line in f if line.strip() and not line.startswith("#")]
        profanity.add_censor_words(custom_words)


class ScanRequest(BaseModel):
    text: str
    user_email: str
    route_id: int


class ScanResponse(BaseModel):
    is_safe: bool
    violation_type: Optional[str] = None
    detected_terms: List[str] = []
    anonymized_text: str


@app.get("/health")
def health_check():
    return {
        "status": "UP",
        "service": "smart-bussing-dlp",
        "datafog_available": DATAFOG_AVAILABLE
    }


@app.post("/api/dlp/scan", response_model=ScanResponse)
def scan_text(request: ScanRequest):
    text = request.text.strip()
    if not text:
        return ScanResponse(is_safe=True, anonymized_text=text)

    # 1. Moderación de Contenido: Detección de Lenguaje Obsceno / Ofensivo
    if profanity.contains_profanity(text):
        censored = profanity.censor(text)
        return ScanResponse(
            is_safe=False,
            violation_type="OBSCENE_CONTENT",
            detected_terms=["Lenguaje ofensivo o inapropiado"],
            anonymized_text=censored
        )

    # 2. DLP con DataFog Core: Detección y Anonimización de PII
    # Evita que usuarios expongan números telefónicos, correos de terceros o tarjetas en reportes públicos
    if DATAFOG_AVAILABLE:
        try:
            findings = datafog.scan(text)
            if findings and len(findings) > 0:
                masked = datafog.transform(text, action="redact")
                return ScanResponse(
                    is_safe=False,
                    violation_type="DLP_PII_LEAK",
                    detected_terms=[str(f) for f in findings],
                    anonymized_text=str(masked)
                )
        except Exception as e:
            # Fallback en caso de error interno del motor DataFog
            pass

    return ScanResponse(
        is_safe=True,
        violation_type=None,
        detected_terms=[],
        anonymized_text=text
    )
