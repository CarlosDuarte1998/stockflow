import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { MessageService } from 'primeng/api';

import { Product } from '../models/product.model';
import { StockAlert } from '../models/alert.model';
import { AlertService } from '../services/alert.service';
import { ProductService } from '../services/product.service';
import { DEFAULT_FILTERS, FILTERS_STORAGE_KEY, InventoryFilters } from './inventory-filters.model';

function cargarFiltrosPersistidos(): InventoryFilters {
  try {
    const raw = localStorage.getItem(FILTERS_STORAGE_KEY);
    return raw ? { ...DEFAULT_FILTERS, ...JSON.parse(raw) } : DEFAULT_FILTERS;
  } catch {
    return DEFAULT_FILTERS;
  }
}

/**
 * Store centralizado del inventario basado en Signals. Concentra el estado que
 * comparten las vistas del dashboard (productos, alertas, seleccion, loading/errores)
 * para evitar prop-drilling y peticiones HTTP duplicadas entre componentes.
 */
@Injectable({ providedIn: 'root' })
export class InventoryStore {
  private readonly productService = inject(ProductService);
  private readonly alertService = inject(AlertService);
  private readonly messageService = inject(MessageService);

  private readonly _products = signal<Product[]>([]);
  private readonly _alerts = signal<StockAlert[]>([]);
  private readonly _selectedProduct = signal<Product | null>(null);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly _filters = signal<InventoryFilters>(cargarFiltrosPersistidos());
  private readonly _totalElements = signal(0);
  private readonly _totalPages = signal(0);
  private readonly _page = signal(0);

  readonly products = this._products.asReadonly();
  readonly alerts = this._alerts.asReadonly();
  readonly selectedProduct = this._selectedProduct.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly filters = this._filters.asReadonly();
  readonly totalElements = this._totalElements.asReadonly();
  readonly totalPages = this._totalPages.asReadonly();
  readonly page = this._page.asReadonly();

  readonly totalProducts = computed(() => this._totalElements());
  readonly totalAlerts = computed(() => this._alerts().length);
  readonly totalCriticalAlerts = computed(
    () => this._alerts().filter((alert) => alert.severity === 'CRITICAL').length
  );
  readonly totalInventoryValue = computed(() =>
    this._products().reduce((suma, producto) => suma + producto.currentStock * producto.unitPrice, 0)
  );

  constructor() {
    // Persistir los filtros activos del usuario en localStorage cada vez que cambian.
    effect(() => {
      localStorage.setItem(FILTERS_STORAGE_KEY, JSON.stringify(this._filters()));
    });

    // Notificar via toast cuando cambia la lista de alertas activas (nuevas alertas detectadas).
    effect(() => {
      const alertas = this._alerts();
      if (alertas.length > 0) {
        const criticas = alertas.filter((alerta) => alerta.severity === 'CRITICAL').length;
        this.messageService.add({
          severity: criticas > 0 ? 'error' : 'warn',
          summary: 'Alertas de inventario',
          detail: `${alertas.length} producto(s) con stock bajo (${criticas} critico(s))`,
          life: 4000
        });
      }
    });
  }

  cargarProductos(pagina = 0, tamano = 10): void {
    this._loading.set(true);
    this._error.set(null);
    const categoria = this._filters().category ?? undefined;

    this.productService.listar(pagina, tamano, categoria).subscribe({
      next: (resultado) => {
        this._products.set(resultado.content);
        this._totalElements.set(resultado.totalElements);
        this._totalPages.set(resultado.totalPages);
        this._page.set(resultado.number);
        this._loading.set(false);
      },
      error: (err) => {
        this._error.set(err?.error?.message ?? 'Error al cargar productos');
        this._loading.set(false);
      }
    });
  }

  cargarAlertas(): void {
    this.alertService.listar().subscribe({
      next: (alertas) => this._alerts.set(alertas),
      error: (err) => this._error.set(err?.error?.message ?? 'Error al cargar alertas')
    });
  }

  seleccionarProducto(producto: Product | null): void {
    this._selectedProduct.set(producto);
  }

  establecerFiltroCategoria(categoria: string | null): void {
    this._filters.set({ ...this._filters(), category: categoria });
    this.cargarProductos(0, this._products().length || 10);
  }

  refrescarTrasMovimiento(): void {
    this.cargarProductos(this._page(), 10);
    this.cargarAlertas();
  }
}
