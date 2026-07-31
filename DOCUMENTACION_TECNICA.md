# StockFlow Inc. — Documentación Técnica

Este documento explica **qué se construyó, por qué se construyó así, y cómo probarlo**, desde los
cimientos del proyecto hasta el estado de las pruebas del frontend. Está pensado como material de
apoyo para la sesión de defensa técnica (60 min) y para entender el proyecto sin tener que leer
todo el código fuente.

> Para instrucciones rápidas de arranque ver el [`README.md`](./README.md) raíz.
> Este documento profundiza en el **por qué** de cada decisión.

---

## 1. Contexto y alcance

**StockFlow Inc.** necesita monitorear inventario en tiempo real: consultar stock, recibir alertas
cuando cae por debajo del mínimo, y registrar movimientos (entradas/salidas).

**Flujo crítico exigido por el enunciado** (debe funcionar end-to-end):
```
Registrar movimiento OUT → actualizar stock del producto → disparar alerta si stock < mínimo
```

Este flujo se implementó y se verificó manualmente contra la API real (ver sección 7).

### Entidades de dominio (tal como las define el enunciado)

```
Product    { id, sku, name, category, currentStock, minStock, unitPrice }
Movement   { id, productId, type[IN/OUT], quantity, reason, timestamp }
StockAlert { productId, productName, currentStock, minStock, severity[LOW/CRITICAL] }
```

### Estructura del monorepo

```
stockflow/
├── inventory-service/   ← Backend Spring Boot 3.5.3
├── inventory-app/       ← Frontend Angular 20 (standalone)
└── README.md            ← instrucciones generales de arranque
```

---

## 2. Backend — `inventory-service`

### 2.1 Stack y arranque

- **Spring Boot 3.5.3**, Java 21, Maven.
- Base de datos **H2 en memoria** (`jdbc:h2:mem:stockflow`), sin configuración externa.
- Arranca con un único comando, sin pasos adicionales:
  ```bash
  cd inventory-service
  mvn spring-boot:run
  ```
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Consola H2: `http://localhost:8080/h2-console`

**¿Por qué Spring Boot 3.5.3 y no 4.x?** El enunciado pide "3.5+". Spring Initializr en este
entorno solo ofrecía 4.x, así que se fijó la 3.5.3 (última estable de la serie 3.5, disponible en
caché local de Maven) para cumplir el requisito sin arrastrar breaking changes de una major que el
enunciado no contemplaba.

### 2.2 Capas y organización del código

```
com.stockflow.inventoryservice
├── domain/            → Product, Movement, StockAlert (+ enums MovementType, AlertSeverity)
├── repository/         → ProductRepository, MovementRepository (Spring Data JPA)
├── dto/                → ProductResponse, MovementRequest/Response, PagedResponse<T>
├── service/            → ProductService, MovementService, AlertService (lógica de negocio)
├── controller/          → ProductController, MovementController, AlertController (solo I/O HTTP)
├── exception/           → excepciones custom + GlobalExceptionHandler + ErrorResponse
└── config/              → WebConfig (CORS), OpenApiConfig, CriticalStockHealthIndicator
```

Se respeta estrictamente `Controller → Service → Repository`: ningún controller contiene lógica de
negocio, solo delega y traduce a `ResponseEntity`.

**Decisión de diseño clave — `StockAlert` no es una entidad persistida.** No hay tabla ni
repositorio para alertas. `AlertService.getActiveAlerts()` calcula las alertas **en vivo** en cada
`GET /api/v1/alerts`, consultando `currentStock <= minStock` directo contra `Product`. Esto es
intencional: el enunciado pide que el flujo "registrar movimiento → actualizar stock → disparar
alerta" funcione end-to-end, y un modelo de solo lectura calculado on-demand lo garantiza sin
necesitar infraestructura de eventos/mensajería para lo que es una API puramente REST.

**Severidad de alerta:**
- `CRITICAL` si `currentStock <= minStock * 0.5`
- `LOW` en cualquier otro caso donde `currentStock <= minStock`

Este mismo umbral (50%) se reutiliza en el health indicator personalizado (sección 2.6).

### 2.3 Endpoints REST

| Endpoint | Método | Descripción |
|---|---|---|
| `/api/v1/products` | GET | Lista productos paginados, filtro opcional `?category=` |
| `/api/v1/products/{id}` | GET | Detalle de un producto |
| `/api/v1/movements` | POST | Registra movimiento (actualiza stock automáticamente) |
| `/api/v1/alerts` | GET | Productos con `currentStock <= minStock` |
| `/api/v1/movements/{productId}/history` | GET | Historial de movimientos de un producto |

**Nota sobre paginación:** inicialmente `GET /products` devolvía el `PageImpl` crudo de Spring
Data (con campos ruido como `pageable`, `sort`, `first`, `last`, `empty`, `numberOfElements` — que
ni el frontend consume ni Spring recomienda serializar directamente, ver warning
`"Serializing PageImpl instances as-is is not supported"`). Se reemplazó por un DTO propio
`PagedResponse<T>` que expone solo `content`, `number`, `size`, `totalElements`, `totalPages`.

### 2.4 Connection Pooling — HikariCP

Configurado explícitamente en `application.yml` (no valores por defecto), con la justificación de
cada parámetro documentada como comentario en el propio YAML:

| Parámetro | Valor | Justificación |
|---|---|---|
| `maximum-pool-size` | 10 | Escenario de carga media: dashboard + registro de movimientos, no picos masivos concurrentes |
| `minimum-idle` | 5 | Mantener la mitad del pool "caliente" evita reabrir conexiones en cada ráfaga sin sobre-reservar en reposo |
| `connection-timeout` | 20000 ms | Fail-fast: no colgar la petición HTTP indefinidamente si el pool está saturado |
| `idle-timeout` | 300000 ms (5 min) | Libera conexiones ociosas sin descartar en cada pausa corta entre peticiones |

### 2.5 Resilience4j — Tolerancia a fallos

Todo configurado en `application.yml`, nada hardcodeado en código:

- **Circuit Breaker** sobre `AlertService.getActiveAlerts()` (`@CircuitBreaker(name = "alertsService", fallbackMethod = "getActiveAlertsFallback")`):
  ventana de 10 llamadas, abre si ≥50% fallan, permanece abierto 10s, luego 3 llamadas de prueba en half-open.
  El fallback loguea la causa y retorna lista vacía (nunca rompe el endpoint).
- **Retry** sobre `MovementService.registerMovement()` (`@Retry(name = "movementRegistration")`):
  máximo 3 intentos, backoff exponencial (500ms → 1000ms).
- **Rate Limiter** sobre el historial de movimientos (`@RateLimiter` en `MovementController`):
  10 peticiones/segundo, sin espera — si se excede, falla inmediato con 429.

Se agregaron además manejadores específicos en `GlobalExceptionHandler` para
`CallNotPermittedException` (→ 503) y `RequestNotPermitted` (→ 429): no estaban explícitamente
pedidos, pero sin ellos esas excepciones caerían en el handler genérico de 500, lo cual sería
incorrecto semánticamente.

### 2.6 Spring Actuator

Expuestos: `/actuator/health`, `/actuator/metrics`, `/actuator/info`.

**Health indicator personalizado** (`CriticalStockHealthIndicator`, bean `"criticalStock"`):
reporta `DOWN` cuando **más del 20%** de los productos están en estado crítico
(`currentStock <= minStock * 0.5`), con detalle de `criticalProducts`, `totalProducts`,
`criticalPercentage` y `threshold`.

Decisión importante: este indicador consulta el repositorio **directamente**, sin pasar por el
circuit breaker de `AlertService`. Así, el chequeo de salud del sistema es independiente del estado
de ese circuito — si el circuito de alertas está abierto, `/actuator/health` sigue siendo confiable.

**`/actuator/info`**: se configuró bajo el namespace `info.app.*` (no `management.info.app.*` —
ese es un error común; el `EnvironmentInfoContributor` de Actuator lee desde `info.*`). También
requirió habilitar explícitamente `management.info.env.enabled: true`, deshabilitado por defecto
desde Spring Boot 2.5+.

### 2.7 Manejo global de excepciones

`GlobalExceptionHandler` (`@RestControllerAdvice`) centraliza todos los errores y responde siempre
con el mismo contrato:

```json
{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "...", "path": "/api/v1/..." }
```

| Excepción | HTTP |
|---|---|
| `ProductNotFoundException` | 404 |
| `InsufficientStockException` | 422 |
| `MethodArgumentNotValidException` (Bean Validation) | 400 |
| `RequestNotPermitted` (rate limiter) | 429 |
| `CallNotPermittedException` (circuit breaker abierto) | 503 |
| `Exception` genérica | 500 |

### 2.8 Documentación OpenAPI / Swagger

Todos los endpoints documentados con `@Operation`, `@ApiResponse` y `@Schema`. Bean `OpenAPI`
(`OpenApiConfig`) con información de contacto, licencia y servidor. Swagger UI accesible en
`/swagger-ui.html` sin autenticación adicional.

### 2.9 Validaciones y datos semilla

- DTOs de entrada (`MovementRequest`) validados con `@Valid` + Bean Validation
  (`@NotNull`, `@Min`, `@Size`).
- `data.sql`: 12 productos en 4 categorías (`ELECTRONICA`, `ALIMENTOS`, `FERRETERIA`, `OFICINA`),
  superando el mínimo pedido (10 productos / 3 categorías).

### 2.10 Pruebas — Backend

**Estado: completo.** 29 tests, **98.8% de cobertura JaCoCo**.

```bash
cd inventory-service
mvn test
# Reporte HTML: target/site/jacoco/index.html
```

| Clase de test | Qué cubre |
|---|---|
| `ProductServiceTest` | listado con/sin filtro de categoría, get por id (found/not found) |
| `MovementServiceTest` | registro IN/OUT, stock insuficiente, historial |
| `AlertServiceTest` | cálculo de severidad, fallback del circuit breaker |
| `GlobalExceptionHandlerTest` | cada excepción → código HTTP correcto |
| `CriticalStockHealthIndicatorTest` | umbral del 20%, casos UP/DOWN/UNKNOWN (sin productos) |
| `InventoryFlowIntegrationTest` | **flujo crítico end-to-end** con `@SpringBootTest` + `MockMvc`: OUT → stock actualizado → alerta visible; más 404/422/400 y paginación |

**Decisión de tooling:** `lombok.config` con `lombok.addLombokGeneratedAnnotation=true` para que
JaCoCo excluya automáticamente getters/setters/builders generados por Lombok, y la métrica de
cobertura refleje lógica de negocio real, no boilerplate.

**Herramientas:** JUnit 5, Mockito (mocks de repositorios/servicios), AssertJ (aserciones),
`MockMvc` + `@SpringBootTest` para integración, JaCoCo para cobertura.

---

## 3. Frontend — `inventory-app`

### 3.1 Stack y arranque

- **Angular 20** (satisface "16+"), 100% standalone components, sin NgModules de feature.
- **PrimeNG 20.4.0** (no-LTS, gratuita — se evitó `20.5.1-lts` porque exige licencia paga y muestra
  un banner de licencia inválida en runtime) con preset Aura.
- **TailwindCSS v4**, integrado con PrimeNG vía CSS `@layer` para evitar conflictos de
  especificidad: `tailwind-base, primeng, tailwind-utilities`.

```bash
cd inventory-app
npm install
ng serve
```

App en `http://localhost:4200`, consume la API en `http://localhost:8080/api/v1`.

**Nota técnica de integración Tailwind v4 + PrimeNG:** hace falta importar **tres** archivos de
Tailwind (`theme.css`, `preflight.css`, `utilities.css`), cada uno en su `@layer` correspondiente.
Omitir `preflight.css` (el reset de estilos base) deja la tipografía y el look general rotos aunque
las utilidades de Tailwind sigan funcionando — fue un bug real detectado y corregido durante el
desarrollo (ver sección 6).

### 3.2 Arquitectura

```
src/app/
├── app.ts / app.html          → shell: sidebar + header dinámico por ruta
├── app.routes.ts               → rutas lazy-loaded (loadComponent)
├── app.config.ts                → providers: Router, HttpClient+interceptor, PrimeNG, MessageService
├── core/
│   ├── models/                  → Product, Movement, StockAlert, Page<T>, ErrorResponse
│   ├── services/                 → ProductService, MovementService, AlertService (inject(HttpClient))
│   ├── interceptors/              → error.interceptor.ts
│   └── store/                     → InventoryStore (Signals)
└── features/
    ├── dashboard/                 → DashboardComponent + AdvancedStatsComponent (@defer on viewport)
    ├── products/                   → ProductsComponent + MovementHistoryComponent (@defer on interaction)
    ├── alerts/                      → AlertsComponent
    └── movements/                    → MovementFormComponent (ReactiveFormsModule)
```

**Routing** (`app.routes.ts`): todas las rutas usan `loadComponent` (lazy), sin módulo de feature.
Ruta raíz redirige a `/dashboard`, wildcard también.

**`inject()`**: usado en los 3 servicios HTTP (`ProductService`, `MovementService`, `AlertService`),
en `InventoryStore`, en el interceptor funcional, y en todos los componentes — supera ampliamente el
mínimo de 3 servicios pedido.

### 3.3 `InventoryStore` — estado centralizado con Signals

Un único servicio (`providedIn: 'root'`) concentra todo el estado compartido entre vistas, evitando
prop-drilling y peticiones HTTP duplicadas:

- **State (signals privados + `.asReadonly()` público):** `products`, `alerts`, `selectedProduct`,
  `loading`, `error`, `filters`, `totalElements`, `totalPages`, `page`.
- **`computed()`:** `totalProducts`, `totalAlerts`, `totalCriticalAlerts`,
  `totalInventoryValue` (`Σ currentStock * unitPrice`).
- **`effect()`** (dos, en el constructor):
  1. Persiste `filters` en `localStorage` cada vez que cambian.
  2. Muestra un toast automático (vía `MessageService`) cuando cambia la lista de alertas,
     diferenciando severidad (`error` si hay críticas, `warn` si no).

### 3.4 Vistas diferidas — `@defer`

| Bloque | Trigger | Placeholder | Loading | Error |
|---|---|---|---|---|
| Historial de movimientos (`MovementHistoryComponent`, dentro de `ProductsComponent`) | `on interaction(historyTrigger)` — clic en "Ver historial" | Texto invitando a hacer clic | Skeleton (`p-skeleton`, min 300ms) | Mensaje de error amigable |
| Estadísticas avanzadas (`AdvancedStatsComponent`, dentro de `DashboardComponent`) | `on viewport` — scroll hasta la sección | Skeletons animados (título + 3 tarjetas) | `p-progressSpinner` centrado | Mensaje de error amigable |

### 3.5 Vistas del dashboard

- **Dashboard**: 4 KPI cards (total productos, alertas activas, alertas críticas, valor total de
  inventario) derivadas de `computed()`, con iconos y colores por métrica. Debajo, estadísticas
  avanzadas con barras de progreso de participación de valor por categoría (`@defer on viewport`).
- **Listado de productos**: tabla PrimeNG (`p-table[lazy]`) con filtro por categoría, paginación
  server-side, columna `#ID` (para referencia manual/pruebas), badge de estado
  OK/BAJO/CRITICO calculado en tiempo real (`stockStatusOf()`), y expansión de historial por fila.
- **Panel de alertas**: tarjetas con icono y color por severidad (ámbar = LOW, rojo = CRITICAL).
- **Registro de movimiento**: formulario reactivo (`ReactiveFormsModule`, `FormBuilder.nonNullable`).
  El campo de producto es un **selector buscable por SKU + nombre** (no un input de ID crudo — ver
  sección 6), que resuelve internamente el `productId` antes de enviar el request. Validaciones
  visibles, botón deshabilitado (`[disabled]="submitting() || form.invalid"`) mientras la petición
  está en vuelo, para evitar envíos duplicados.

### 3.6 Manejo de errores y UX

- **`error.interceptor.ts`** (interceptor funcional, `HttpInterceptorFn`): parsea el `ErrorResponse`
  consistente del backend (`timestamp, status, error, message, path`) y muestra un toast descriptivo
  vía `MessageService`, con fallback si el body no tiene el shape esperado.
- **Skeleton loaders** (`p-skeleton`) durante peticiones HTTP activas (tabla de productos,
  historial, estadísticas avanzadas).
- Botón de registro de movimiento deshabilitado durante el submit.

### 3.7 Diseño visual

El shell usa **sidebar fijo** (con branding e iconos `pi-*` de PrimeIcons) en vez de barra superior,
con header dinámico que muestra título/subtítulo según la ruta activa y la fecha actual en español
(vía `Intl.DateTimeFormat('es-ES', ...)`, sin depender de datos de locale de Angular no registrados
— ver bug corregido en sección 6). Las 4 vistas comparten un lenguaje visual consistente: tarjetas
con borde suave, iconos circulares de color por contexto, y badges de estado.

---

## 4. Pruebas del Frontend — estado y plan

### Estado actual

**Pendiente de implementar.** El backend ya cumple el 70% de cobertura exigido (98.8% con JaCoCo);
el frontend usa el stack de testing por defecto de Angular (**Jasmine + Karma**, ya incluido en el
proyecto vía `ng generate`, sin dependencias adicionales que instalar) pero los specs todavía no se
han escrito.

### Cómo se van a correr

```bash
cd inventory-app
ng test --no-watch --code-coverage
# Reporte HTML: coverage/inventory-app/index.html
```

### Plan de cobertura (qué se va a testear y por qué)

| Área | Qué se prueba | Técnica |
|---|---|---|
| `InventoryStore` | `loadProducts()`/`loadAlerts()` (éxito y error), los 4 `computed()`, los 2 `effect()` (persistencia en `localStorage`, toast en cambio de alertas) | Mocks de `ProductService`/`AlertService` con `jasmine.createSpyObj`, `MessageService` real o spy |
| `stockStatusOf()` (`product.model.ts`) | función pura: casos límite exactos (`currentStock === minStock`, `=== minStock*0.5`, por encima/por debajo) | Test unitario puro, sin `TestBed` |
| `error.interceptor.ts` | parseo correcto del `ErrorResponse` del backend, fallback cuando el body no tiene el shape esperado, toast disparado con severidad `error` | `HttpClientTestingModule` + `HttpTestingController`, spy sobre `MessageService` |
| `ProductService` / `MovementService` / `AlertService` | URL, query params (`page`, `size`, `category`) y payload correctos en cada llamada | `HttpClientTestingModule` |
| `MovementFormComponent` | validación del form (campos requeridos, `min(1)`), resolución de SKU → `productId`, botón deshabilitado durante `submitting()`, reset tras éxito | `TestBed.configureTestingModule` con `ReactiveFormsModule`, spies de servicios |
| `ProductsComponent` | guard de paginación (evita recargas redundantes en `onLazyLoad`), cálculo de severidad/icono por producto | Spy sobre `InventoryStore` |
| Resto de componentes (`AlertsComponent`, `DashboardComponent`, `AlertsComponent`) | smoke tests: crean, no explotan, renderizan el estado vacío/con datos | `TestBed` básico |

**Por qué este orden de prioridad:** el store y los pipes/funciones puras concentran la lógica de
negocio del frontend (equivalente a la capa de "service" del backend), así que se priorizan sobre
smoke tests de componentes visuales, siguiendo el mismo criterio de "probar lógica, no
boilerplate" aplicado en el backend con la exclusión de Lombok.

---

## 5. El flujo crítico, de punta a punta

El enunciado exige que este flujo funcione completo: **registrar movimiento de salida → actualizar
stock → disparar alerta si cae bajo el mínimo.**

1. **Frontend**: `MovementFormComponent.submit()` arma un `MovementRequest { productId, type: 'OUT', quantity, reason }` (resolviendo `productId` desde el SKU elegido) y llama a `MovementService.register()`.
2. **Backend — `POST /api/v1/movements`**: `MovementController` delega a `MovementService.registerMovement()` (protegido con `@Retry`).
3. **`MovementService`**: obtiene el `Product`, valida stock suficiente (si no, `InsufficientStockException` → 422), calcula el nuevo `currentStock`, persiste el `Movement`, todo dentro de `@Transactional`.
4. **Frontend**: al recibir éxito, `InventoryStore.refreshAfterMovement()` recarga productos y alertas.
5. **Backend — `GET /api/v1/alerts`**: `AlertService.getActiveAlerts()` recalcula en vivo contra la tabla `Product` — si el nuevo `currentStock <= minStock`, el producto aparece con severidad `LOW` o `CRITICAL` según el umbral del 50%.
6. **Frontend**: el `effect()` de `InventoryStore` detecta el cambio en `alerts` y dispara un toast automático.

Verificado manualmente contra la API real (no solo con tests): ver ejemplo completo en la sección 7.

---

## 6. Bugs reales encontrados y corregidos durante el desarrollo

Documentados aquí porque son parte de las decisiones técnicas y muestran el proceso de
verificación, no solo el resultado final.

1. **Banner de licencia inválida de PrimeNG.** `primeng@20.5.1-lts` requiere licencia paga y
   muestra un banner en el navegador. Corregido bajando a `primeng@20.4.0` (gratuita, verificada
   compatible con Angular 20 vía `npm view primeng@20.4.0 peerDependencies`).
2. **Tailwind "no se aplicaba" / estilos rotos.** Faltaba `@import "tailwindcss/preflight.css" layer(tailwind-base);` en `styles.css`. Sin el reset base, la tipografía quedaba en serif por defecto del navegador aunque las utilidades de layout (`flex`, `p-6`, etc.) sí funcionaran. Diagnosticado comparando screenshots de Chrome headless antes/después.
3. **Loop infinito en `/products`.** La tabla (`p-table[lazy]`) estaba envuelta en `@if (loading) {...} @else {<p-table>}`. Como `loading` cambia en cada ciclo de carga y la tabla se desmontaba/remontaba con ese `@if`, cada remontaje volvía a disparar `onLazyLoad`, entrando en loop. Corregido usando el `[loading]` nativo de `p-table` sin desmontar el componente.
4. **`NG0701: Missing locale data for the locale "es"`.** Al instanciar `new CurrencyPipe('es')` manualmente en el dashboard (sin registrar los datos de locale de Angular), la excepción rompía silenciosamente el render de todo el bloque de KPIs. Corregido con `Intl.NumberFormat` nativo, que no depende del registro de locales de Angular.
5. **Falta de CORS.** El backend no tenía configuración CORS; `curl` no lo detecta (no aplica same-origin policy), lo que dio una falsa sensación de que todo funcionaba hasta probar desde el navegador real. Corregido con `WebConfig` (`WebMvcConfigurer.addCorsMappings`) permitiendo `http://localhost:4200`.
6. **`Page` crudo de Spring Data expuesto en la API.** Ver sección 2.3 — reemplazado por `PagedResponse<T>`.
7. **UX del formulario de movimiento pedía un `productId` numérico que el usuario no podía conocer** (la tabla de productos no mostraba el id). Corregido en dos frentes: el formulario ahora busca por SKU/nombre (resolviendo el id internamente, sin cambiar el contrato del backend), y la tabla de productos ahora muestra explícitamente la columna `#ID` como referencia.

---

## 7. Verificación manual contra la API real (ejemplo)

```bash
# Estado inicial: Monitor 24" (id=3), stock=15, minStock=5
curl http://localhost:8080/api/v1/products/3

# Registrar OUT de 12 unidades
curl -X POST http://localhost:8080/api/v1/movements \
  -H "Content-Type: application/json" \
  -d '{"productId":3,"type":"OUT","quantity":12,"reason":"Venta de prueba"}'

# Stock actualizado: 15 -> 3 (por debajo del minimo)
curl http://localhost:8080/api/v1/products/3

# La alerta aparece de inmediato, severidad LOW (3 > 5*0.5)
curl http://localhost:8080/api/v1/alerts
```

Casos de error verificados: `404` (producto inexistente), `422` (stock insuficiente),
`400` (validación Bean Validation). Health indicator verificado en `DOWN` con datos semilla
reales (3/12 productos críticos = 25% > umbral 20%).

---

## 8. Decisiones de commits y flujo de trabajo

- Repositorio Git local, commits agrupados por feature/fix (no un commit por archivo, pero cada
  corrección relevante en su propio commit) con mensajes en español, salvo términos técnicos de uso
  estándar en inglés (`feat`, `fix`, `commit`, etc.), según lo acordado con el evaluador del
  desarrollo del proyecto.
- Cada cambio se verificó antes de commitear: `mvn test` (backend), `ng build` (frontend), y en
  cambios de UI, captura de pantalla real vía Chrome headless para confirmar el render (no solo
  que compile).

---

## 9. Pendientes

| Ítem | Estado |
|---|---|
| Pruebas unitarias frontend (≥70% cobertura) | **Pendiente** — plan detallado en sección 4 |
| README raíz: tiempo invertido por módulo | Pendiente de completar con horas reales |
| Envío de correo con URL del repo a los evaluadores | Acción del candidato, no de desarrollo |
