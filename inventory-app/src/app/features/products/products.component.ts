import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { ButtonModule } from 'primeng/button';

import { Product, StockStatus, estadoStockDe } from '../../core/models/product.model';
import { InventoryStore } from '../../core/store/inventory.store';
import { MovementHistoryComponent } from './movement-history.component';

const SEVERIDAD_POR_ESTADO: Record<StockStatus, 'success' | 'warn' | 'danger'> = {
  OK: 'success',
  BAJO: 'warn',
  CRITICO: 'danger'
};

const ICONO_POR_ESTADO: Record<StockStatus, string> = {
  OK: 'pi pi-check',
  BAJO: 'pi pi-exclamation-circle',
  CRITICO: 'pi pi-times-circle'
};

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [CurrencyPipe, FormsModule, TableModule, TagModule, SelectModule, SkeletonModule, ButtonModule, MovementHistoryComponent],
  templateUrl: './products.component.html'
})
export class ProductsComponent implements OnInit {
  protected readonly store = inject(InventoryStore);

  protected readonly categories = [
    { label: 'Todas', value: null },
    { label: 'Electronica', value: 'ELECTRONICA' },
    { label: 'Alimentos', value: 'ALIMENTOS' },
    { label: 'Ferreteria', value: 'FERRETERIA' },
    { label: 'Oficina', value: 'OFICINA' }
  ];

  protected expandedProductId: number | null = null;

  private lastRows = 10;

  ngOnInit(): void {
    this.store.cargarProductos(0, this.lastRows);
  }

  alCambiarPagina(event: TableLazyLoadEvent): void {
    const filas = event.rows ?? 10;
    const pagina = Math.floor((event.first ?? 0) / filas);

    // Evita recargas redundantes si p-table reemite onLazyLoad para el mismo estado
    // de paginacion ya cargado (por ejemplo, al inicializarse tras la carga de ngOnInit).
    if (pagina === this.store.page() && filas === this.lastRows) {
      return;
    }

    this.lastRows = filas;
    this.store.cargarProductos(pagina, filas);
  }

  alCambiarCategoria(categoria: string | null): void {
    this.store.establecerFiltroCategoria(categoria);
  }

  alternarHistorial(producto: Product): void {
    this.expandedProductId = this.expandedProductId === producto.id ? null : producto.id;
    this.store.seleccionarProducto(producto);
  }

  estadoDe(producto: Product): StockStatus {
    return estadoStockDe(producto);
  }

  severidadDe(producto: Product) {
    return SEVERIDAD_POR_ESTADO[this.estadoDe(producto)];
  }

  iconoDe(producto: Product): string {
    return ICONO_POR_ESTADO[this.estadoDe(producto)];
  }
}
