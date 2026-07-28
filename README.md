# StockFlow — Prueba Técnica Fullstack

Aplicación de monitoreo de inventario en tiempo real para StockFlow Inc.

Monorepo compuesto por:

- [`inventory-service/`](./inventory-service) — Microservicio backend (Spring Boot 3.5+).
- [`inventory-app/`](./inventory-app) — SPA de dashboard (Angular 16+).

## Cómo levantar el proyecto

### Backend

```bash
cd inventory-service
mvn spring-boot:run
```

API disponible en `http://localhost:8080`. Swagger UI en `http://localhost:8080/swagger-ui.html`.

### Frontend

```bash
cd inventory-app
npm install
ng serve
```

App disponible en `http://localhost:4200`.

## Tiempo invertido por módulo

_(se completa al finalizar el desarrollo)_

| Módulo | Tiempo invertido |
|---|---|
| inventory-service | pendiente |
| inventory-app | pendiente |
| Documentación / integración | pendiente |

## Decisiones técnicas relevantes

_(se completa progresivamente durante el desarrollo)_
