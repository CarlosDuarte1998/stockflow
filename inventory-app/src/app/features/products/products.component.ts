import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { ButtonModule } from 'primeng/button';

import { Product, StockStatus, stockStatusOf } from '../../core/models/product.model';
import { InventoryStore } from '../../core/store/inventory.store';
import { MovementHistoryComponent } from './movement-history.component';

const STATUS_SEVERITY: Record<StockStatus, 'success' | 'warn' | 'danger'> = {
  OK: 'success',
  BAJO: 'warn',
  CRITICO: 'danger'
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

  ngOnInit(): void {
    this.store.loadProducts(0, 10);
  }

  onPageChange(event: TableLazyLoadEvent): void {
    const page = Math.floor((event.first ?? 0) / (event.rows ?? 10));
    this.store.loadProducts(page, event.rows ?? 10);
  }

  onCategoryChange(category: string | null): void {
    this.store.setCategoryFilter(category);
  }

  toggleHistory(product: Product): void {
    this.expandedProductId = this.expandedProductId === product.id ? null : product.id;
    this.store.selectProduct(product);
  }

  statusOf(product: Product): StockStatus {
    return stockStatusOf(product);
  }

  severityOf(product: Product) {
    return STATUS_SEVERITY[this.statusOf(product)];
  }
}
