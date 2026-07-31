import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectModule } from 'primeng/select';

import { InventoryStore } from '../../core/store/inventory.store';
import { MovementService } from '../../core/services/movement.service';
import { ProductService } from '../../core/services/product.service';

interface OpcionProducto {
  label: string;
  value: number;
}

@Component({
  selector: 'app-movement-form',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonModule, InputTextModule, InputNumberModule, SelectModule],
  templateUrl: './movement-form.component.html'
})
export class MovementFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly movementService = inject(MovementService);
  private readonly productService = inject(ProductService);
  private readonly messageService = inject(MessageService);
  private readonly store = inject(InventoryStore);

  protected readonly submitting = signal(false);
  protected readonly productOptions = signal<OpcionProducto[]>([]);
  protected readonly loadingProducts = signal(true);

  protected readonly types = [
    { label: 'Entrada (IN)', value: 'IN' },
    { label: 'Salida (OUT)', value: 'OUT' }
  ];

  protected readonly form = this.fb.nonNullable.group({
    productId: [null as number | null, [Validators.required, Validators.min(1)]],
    type: ['OUT' as 'IN' | 'OUT', Validators.required],
    quantity: [null as number | null, [Validators.required, Validators.min(1)]],
    reason: ['', Validators.maxLength(255)]
  });

  ngOnInit(): void {
    this.productService.listar(0, 100).subscribe({
      next: (pagina) => {
        this.productOptions.set(
          pagina.content
            .map((producto) => ({ label: `${producto.sku} — ${producto.name}`, value: producto.id }))
            .sort((a, b) => a.label.localeCompare(b.label))
        );
        this.loadingProducts.set(false);
      },
      error: () => this.loadingProducts.set(false)
    });
  }

  enviar(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const valor = this.form.getRawValue();

    this.movementService
      .registrar({
        productId: valor.productId!,
        type: valor.type,
        quantity: valor.quantity!,
        reason: valor.reason || undefined
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.messageService.add({
            severity: 'success',
            summary: 'Movimiento registrado',
            detail: 'El stock del producto fue actualizado correctamente.'
          });
          this.form.reset({ productId: null, type: 'OUT', quantity: null, reason: '' });
          this.store.refrescarTrasMovimiento();
        },
        error: () => {
          this.submitting.set(false);
        }
      });
  }
}
