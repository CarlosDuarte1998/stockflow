import { Component, OnInit, computed, inject } from '@angular/core';
import { SkeletonModule } from 'primeng/skeleton';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { InventoryStore } from '../../core/store/inventory.store';
import { AdvancedStatsComponent } from './advanced-stats.component';

interface Indicador {
  etiqueta: string;
  valor: string;
  icono: string;
  color: string;
  pista?: string;
}

const FORMATEADOR_MONEDA = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [SkeletonModule, ProgressSpinnerModule, AdvancedStatsComponent],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  protected readonly store = inject(InventoryStore);

  protected readonly indicadores = computed<Indicador[]>(() => [
    {
      etiqueta: 'Total de productos',
      valor: `${this.store.totalProducts()}`,
      icono: 'pi-box',
      color: '#3b82f6'
    },
    {
      etiqueta: 'Alertas activas',
      valor: `${this.store.totalAlerts()}`,
      icono: 'pi-bell',
      color: '#f59e0b',
      pista: this.store.totalAlerts() > 0 ? 'Requieren revision' : 'Todo en orden'
    },
    {
      etiqueta: 'Alertas criticas',
      valor: `${this.store.totalCriticalAlerts()}`,
      icono: 'pi-exclamation-triangle',
      color: '#ef4444',
      pista: this.store.totalCriticalAlerts() > 0 ? 'Accion inmediata' : 'Sin criticos'
    },
    {
      etiqueta: 'Valor total de inventario',
      valor: FORMATEADOR_MONEDA.format(this.store.totalInventoryValue()),
      icono: 'pi-dollar',
      color: '#22c55e'
    }
  ]);

  ngOnInit(): void {
    // El dashboard necesita el catalogo completo (no solo una pagina) para que el KPI de
    // valor total y el desglose por categoria reflejen todos los productos, no solo los
    // primeros N. La vista de Productos vuelve a pedir su propia pagina al navegar ahi.
    this.store.cargarProductos(0, 100);
    this.store.cargarAlertas();
  }
}
