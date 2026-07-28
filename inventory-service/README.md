# inventory-service

Microservicio Spring Boot 3.5+ que expone la API REST de inventario para StockFlow Inc.

## Requisitos

- Java 21+
- Maven 3.9+

## Ejecutar

```bash
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Actuator health: `http://localhost:8080/actuator/health`
- Consola H2: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:stockflow`, user `sa`, sin password)

## Tests

```bash
mvn test
```

El reporte de cobertura (JaCoCo) se genera en `target/site/jacoco/index.html`.
