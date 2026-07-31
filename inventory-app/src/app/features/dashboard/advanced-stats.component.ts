import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { ProgressBarModule } from 'primeng/progressbar';

import { InventoryStore } from '../../core/store/inventory.store';

interface DesglosePorCategoria {
  categoria: string;
  cantidadProductos: number;
  stockTotal: number;
  valorTotal: number;
  participacionValor: number;
}

@Component({
  selector: 'app-advanced-stats',
  standalone: true,
  imports: [ProgressBarModule, CurrencyPipe, DecimalPipe],
  templateUrl: './advanced-stats.component.html'
})
export class AdvancedStatsComponent {
  private readonly store = inject(InventoryStore);

  protected readonly desglose = computed<DesglosePorCategoria[]>(() => {
    const porCategoria = new Map<string, Omit<DesglosePorCategoria, 'participacionValor'>>();

    for (const producto of this.store.products()) {
      const entrada = porCategoria.get(producto.category) ?? {
        categoria: producto.category,
        cantidadProductos: 0,
        stockTotal: 0,
        valorTotal: 0
      };
      entrada.cantidadProductos += 1;
      entrada.stockTotal += producto.currentStock;
      entrada.valorTotal += producto.currentStock * producto.unitPrice;
      porCategoria.set(producto.category, entrada);
    }

    const totalGeneral = [...porCategoria.values()].reduce((suma, entrada) => suma + entrada.valorTotal, 0);

    return [...porCategoria.values()]
      .map((entrada) => ({
        ...entrada,
        participacionValor: totalGeneral > 0 ? (entrada.valorTotal / totalGeneral) * 100 : 0
      }))
      .sort((a, b) => b.valorTotal - a.valorTotal);
  });

  protected readonly stockPromedioPorProducto = computed(() => {
    const productos = this.store.products();
    if (productos.length === 0) {
      return 0;
    }
    return productos.reduce((suma, producto) => suma + producto.currentStock, 0) / productos.length;
  });
}
