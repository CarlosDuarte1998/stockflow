import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { CardModule } from 'primeng/card';

import { InventoryStore } from '../../core/store/inventory.store';

interface CategoryBreakdown {
  category: string;
  productCount: number;
  totalStock: number;
  totalValue: number;
}

@Component({
  selector: 'app-advanced-stats',
  standalone: true,
  imports: [CardModule, CurrencyPipe, DecimalPipe],
  templateUrl: './advanced-stats.component.html'
})
export class AdvancedStatsComponent {
  private readonly store = inject(InventoryStore);

  protected readonly breakdown = computed<CategoryBreakdown[]>(() => {
    const byCategory = new Map<string, CategoryBreakdown>();

    for (const product of this.store.products()) {
      const entry = byCategory.get(product.category) ?? {
        category: product.category,
        productCount: 0,
        totalStock: 0,
        totalValue: 0
      };
      entry.productCount += 1;
      entry.totalStock += product.currentStock;
      entry.totalValue += product.currentStock * product.unitPrice;
      byCategory.set(product.category, entry);
    }

    return [...byCategory.values()].sort((a, b) => b.totalValue - a.totalValue);
  });

  protected readonly averageStockPerProduct = computed(() => {
    const products = this.store.products();
    if (products.length === 0) {
      return 0;
    }
    return products.reduce((sum, p) => sum + p.currentStock, 0) / products.length;
  });
}
