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
  readonly idProducto = input.required<number>();

  private readonly movementService = inject(MovementService);

  protected movimientos: Movement[] = [];
  protected cargando = true;
  protected errorCarga = false;

  ngOnInit(): void {
    this.movementService.historial(this.idProducto()).subscribe({
      next: (movimientos) => {
        this.movimientos = movimientos;
        this.cargando = false;
      },
      error: () => {
        this.errorCarga = true;
        this.cargando = false;
      }
    });
  }
}
