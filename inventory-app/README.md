# inventory-app

SPA Angular 20 (standalone, satisface el requisito "Angular 16+") con PrimeNG y TailwindCSS
para el dashboard de monitoreo de inventario de StockFlow Inc.

## Requisitos

- Node.js 20+
- npm 10+

## Ejecutar

```bash
npm install
ng serve
```

App disponible en `http://localhost:4200`. Consume la API de `inventory-service` en
`http://localhost:8080`.

## Stack

- Angular 20, Standalone Components, Signals.
- PrimeNG 20 (preset Aura) para los componentes de UI.
- TailwindCSS 4 para utilidades de layout/spacing, coexistiendo con PrimeNG via CSS layers
  (`tailwind-base`, `primeng`, `tailwind-utilities`) para evitar conflictos de especificidad.

## Tests

```bash
ng test
```
