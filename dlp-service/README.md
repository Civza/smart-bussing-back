# Smart Bussing DLP & Moderation Service (DataFog Core)

Microservicio complementario (patrón Sidecar) desarrollado para la práctica de DLP de CETYS Universidad.
Provee inspección de datos confidenciales (PII) mediante **DataFog Core** y moderación de contenido ofensivo para la API de reportes ciudadanos de Smart Bussing.

---

## 1. Requisitos e Instalación Local

```bash
# Entrar al directorio
cd dlp-service

# Crear entorno virtual de Python
python -m venv venv
venv\Scripts\activate   # En Windows
# source venv/bin/activate  # En Linux/macOS

# Instalar dependencias
pip install -r requirements.txt
```

---

## 2. Ejecución

```bash
uvicorn main:app --host 127.0.0.1 --port 8000 --reload
```
El servicio estará disponible en `http://127.0.0.1:8000`.
Documentación interactiva Swagger: `http://127.0.0.1:8000/docs`.

---

## 3. Ejecución con Docker

```bash
docker build -t smart-bussing-dlp .
docker run -p 8000:8000 smart-bussing-dlp
```

---

## 4. Prueba del Endpoint de Escaneo

```bash
curl -X POST http://127.0.0.1:8000/api/dlp/scan \
  -H "Content-Type: application/json" \
  -d '{"text":"El chofer es un incompetente y grosero","user_email":"test@cetys.mx","route_id":1}'
```

Respuesta esperada:
```json
{
  "is_safe": false,
  "violation_type": "OBSCENE_CONTENT",
  "detected_terms": ["Lenguaje ofensivo o inapropiado"],
  "anonymized_text": "El chofer es un **** y grosero"
}
```

---

## 5. Anonimización de Exportaciones Históricas

Para anonimizar un archivo CSV de reportes antes de compartirlo con terceros o analistas:

```bash
python export_anonymizer.py reportes_origen.csv reportes_anonimizados.csv
```
