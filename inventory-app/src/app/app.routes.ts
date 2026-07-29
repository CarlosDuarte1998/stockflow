import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'dashboard'
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent)
  },
  {
    path: 'products',
    loadComponent: () =>
      import('./features/products/products.component').then((m) => m.ProductsComponent)
  },
  {
    path: 'alerts',
    loadComponent: () =>
      import('./features/alerts/alerts.component').then((m) => m.AlertsComponent)
  },
  {
    path: 'movements',
    loadComponent: () =>
      import('./features/movements/movement-form.component').then((m) => m.MovementFormComponent)
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
