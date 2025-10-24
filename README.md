
# AstroKiddo (Teacher Deck Builder) — Spring Boot + Angular

Generate 5-slide mini-lessons from NASA media by topic, then export as **HTML** or **PDF**.

## Stack
- **Backend:** Java 17, Spring Boot 3 (Web, WebFlux WebClient, Validation, OpenAPI), OpenHTMLToPDF (HTML→PDF)
- **NASA APIs:** APOD, NASA Image & Video Library
- **Frontend:** Angular 17 (standalone), file download via `file-saver`
- **Dev:** Dockerfiles for both services, proxy for `/api`