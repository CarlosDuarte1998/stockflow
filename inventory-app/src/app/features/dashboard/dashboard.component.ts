import { Component, OnInit, computed, inject } from '@angular/core';
import { SkeletonModule } from 'primeng/skeleton';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { InventoryStore } from '../../core/store/inventory.store';
import { AdvancedStatsComponent } from './advanced-stats.component';

interface Kpi {
  label: string;
  value: string;
  icon: string;
  color: string;
  hint?: string;
}

const CURRENCY_FORMATTER = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [SkeletonModule, ProgressSpinnerModule, AdvancedStatsComponent],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  protected readonly store = inject(InventoryStore);

  protected readonly kpis = computed<Kpi[]>(() => [
    {
      label: 'Total de productos',
      value: `${this.store.totalProducts()}`,
      icon: 'pi-box',
      color: '#3b82f6'
    },
    {
      label: 'Alertas activas',
      value: `${this.store.totalAlerts()}`,
      icon: 'pi-bell',
      color: '#f59e0b',
      hint: this.store.totalAlerts() > 0 ? 'Requieren revision' : 'Todo en orden'
    },
    {
      label: 'Alertas criticas',
      value: `${this.store.totalCriticalAlerts()}`,
      icon: 'pi-exclamation-triangle',
      color: '#ef4444',
      hint: this.store.totalCriticalAlerts() > 0 ? 'Accion inmediata' : 'Sin criticos'
    },
    {
      label: 'Valor total de inventario',
      value: CURRENCY_FORMATTER.format(this.store.totalInventoryValue()),
      icon: 'pi-dollar',
      color: '#22c55e'
    }
  ]);

  ngOnInit(): void {
    this.store.loadProducts();
    this.store.loadAlerts();
  }
}
