import { Component, OnInit, inject } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { CardModule } from 'primeng/card';
import { SkeletonModule } from 'primeng/skeleton';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { InventoryStore } from '../../core/store/inventory.store';
import { AdvancedStatsComponent } from './advanced-stats.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CardModule, CurrencyPipe, SkeletonModule, ProgressSpinnerModule, AdvancedStatsComponent],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  protected readonly store = inject(InventoryStore);

  ngOnInit(): void {
    this.store.loadProducts();
    this.store.loadAlerts();
  }
}
