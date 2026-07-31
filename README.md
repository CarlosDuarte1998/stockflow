# StockFlow — Prueba Técnica Fullstack

Aplicación de monitoreo de inventario en tiempo real para StockFlow Inc.

Monorepo compuesto por:

- [`inventory-service/`](./inventory-service) — Microservicio backend (Spring Boot 3.5+).
- [`inventory-app/`](./inventory-app) — SPA de dashboard (Angular 16+).

📖 **[DOCUMENTACION_TECNICA.md](./DOCUMENTACION_TECNICA.md)** — explicación completa de la
arquitectura, decisiones técnicas, endpoints, Resilience4j, Signals, `@defer`, bugs encontrados y
corregidos, y el plan/estado de pruebas del frontend. Material de apoyo para la defensa técnica.

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

## Estado actual

- `inventory-service`: base completa (endpoints REST, HikariCP, Resilience4j, Actuator,
  OpenAPI/Swagger, manejo global de excepciones, tests con 98.8% cobertura JaCoCo).
- `inventory-app`: pendiente, se retoma en una siguiente sesion.

## Tiempo invertido por módulo

_(se completa al finalizar el desarrollo)_

| Módulo | Tiempo invertido |
|---|---|
| inventory-service | pendiente de registrar horas exactas |
| inventory-app | pendiente |
| Documentación / integración | pendiente |

## Decisiones técnicas relevantes

### inventory-service

- **Spring Boot 3.5.3**: se fijó esta version puntual (dentro del rango "3.5+" pedido) por ser
  la ultima release estable de la serie 3.5 disponible en Maven Central al momento de desarrollar;
  se evito saltar a Spring Boot 4.x para no introducir cambios de breaking cambios no cubiertos
  por el enunciado.
- **StockAlert como modelo de solo lectura, no entidad JPA**: las alertas se calculan en vivo
  contra `Product` (`currentStock <= minStock`) en cada `GET /api/v1/alerts` en lugar de
  persistirse o despacharse como eventos. Esto satisface el flujo pedido (registrar movimiento
  -> actualizar stock -> disparar alerta) sin necesitar infraestructura de mensajeria para una
  API puramente REST.
- **Severity LOW/CRITICAL**: CRITICAL cuando `currentStock <= minStock * 0.5` (o es 0), LOW en el
  resto de los casos por debajo del minimo. Umbral documentado en `AlertService` y
  `CriticalStockHealthIndicator`.
- **HikariCP**: pool dimensionado para carga media (10 max, 5 min-idle) con la justificacion de
  cada valor como comentario en `application.yml`.
- **Resilience4j**: Circuit Breaker sobre `AlertService.getActiveAlerts()` (fallback a lista
  vacia), Retry con backoff exponencial sobre `MovementService.registerMovement()`, Rate Limiter
  (10 req/s) sobre el endpoint de historial. Se agregaron ademas manejadores especificos en el
  `@RestControllerAdvice` para `CallNotPermittedException` (503) y `RequestNotPermitted` (429),
  no exigidos explicitamente pero necesarios para que esas excepciones no caigan en el handler
  generico de 500.
- **Health indicator personalizado** (`criticalStock`): consulta directamente el repositorio
  (no pasa por el circuit breaker de alertas) para que el chequeo de salud sea independiente del
  estado de ese circuito.
- **Cobertura de tests**: se configuro `lombok.config` con
  `lombok.addLombokGeneratedAnnotation=true` para que JaCoCo excluya automaticamente el codigo
  generado por Lombok (getters/setters/builders) y la metrica de cobertura refleje la logica de
  negocio real, no boilerplate.
