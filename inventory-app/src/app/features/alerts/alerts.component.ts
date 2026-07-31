import { Component, OnInit, inject } from '@angular/core';
import { TagModule } from 'primeng/tag';

import { InventoryStore } from '../../core/store/inventory.store';

@Component({
  selector: 'app-alerts',
  standalone: true,
  imports: [TagModule],
  templateUrl: './alerts.component.html'
})
export class AlertsComponent implements OnInit {
  protected readonly store = inject(InventoryStore);

  ngOnInit(): void {
    this.store.loadAlerts();
  }
}
