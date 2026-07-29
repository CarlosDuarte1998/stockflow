import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { MessageService } from 'primeng/api';

import { Product } from '../models/product.model';
import { StockAlert } from '../models/alert.model';
import { AlertService } from '../services/alert.service';
import { ProductService } from '../services/product.service';
import { DEFAULT_FILTERS, FILTERS_STORAGE_KEY, InventoryFilters } from './inventory-filters.model';

function loadPersistedFilters(): InventoryFilters {
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
  private readonly _filters = signal<InventoryFilters>(loadPersistedFilters());
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
    this._products().reduce((sum, product) => sum + product.currentStock * product.unitPrice, 0)
  );

  constructor() {
    // Persistir los filtros activos del usuario en localStorage cada vez que cambian.
    effect(() => {
      localStorage.setItem(FILTERS_STORAGE_KEY, JSON.stringify(this._filters()));
    });

    // Notificar via toast cuando cambia la lista de alertas activas (nuevas alertas detectadas).
    effect(() => {
      const alerts = this._alerts();
      if (alerts.length > 0) {
        const critical = alerts.filter((alert) => alert.severity === 'CRITICAL').length;
        this.messageService.add({
          severity: critical > 0 ? 'error' : 'warn',
          summary: 'Alertas de inventario',
          detail: `${alerts.length} producto(s) con stock bajo (${critical} critico(s))`,
          life: 4000
        });
      }
    });
  }

  loadProducts(page = 0, size = 10): void {
    this._loading.set(true);
    this._error.set(null);
    const category = this._filters().category ?? undefined;

    this.productService.list(page, size, category).subscribe({
      next: (result) => {
        this._products.set(result.content);
        this._totalElements.set(result.totalElements);
        this._totalPages.set(result.totalPages);
        this._page.set(result.number);
        this._loading.set(false);
      },
      error: (err) => {
        this._error.set(err?.error?.message ?? 'Error al cargar productos');
        this._loading.set(false);
      }
    });
  }

  loadAlerts(): void {
    this.alertService.list().subscribe({
      next: (alerts) => this._alerts.set(alerts),
      error: (err) => this._error.set(err?.error?.message ?? 'Error al cargar alertas')
    });
  }

  selectProduct(product: Product | null): void {
    this._selectedProduct.set(product);
  }

  setCategoryFilter(category: string | null): void {
    this._filters.set({ ...this._filters(), category });
    this.loadProducts(0, this._products().length || 10);
  }

  refreshAfterMovement(): void {
    this.loadProducts(this._page(), 10);
    this.loadAlerts();
  }
}
