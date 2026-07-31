import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, input } from '@angular/core';
import { TagModule } from 'primeng/tag';
import { SkeletonModule } from 'primeng/skeleton';

import { Movement } from '../../core/models/movement.model';
import { MovementService } from '../../core/services/movement.service';

@Component({
  selector: 'app-movement-history',
  standalone: true,
  imports: [DatePipe, TagModule, SkeletonModule],
  templateUrl: './movement-history.component.html'
})
export class MovementHistoryComponent implements OnInit {
  readonly productId = input.required<number>();

  private readonly movementService = inject(MovementService);

  protected movements: Movement[] = [];
  protected loading = true;
  protected loadError = false;

  ngOnInit(): void {
    this.movementService.history(this.productId()).subscribe({
      next: (movements) => {
        this.movements = movements;
        this.loading = false;
      },
      error: () => {
        this.loadError = true;
        this.loading = false;
      }
    });
  }
}
