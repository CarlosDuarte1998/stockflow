import { Component, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ToastModule } from 'primeng/toast';
import { filter } from 'rxjs';

interface NavItem {
  path: string;
  label: string;
  icon: string;
  title: string;
  subtitle: string;
}

const NAV_ITEMS: NavItem[] = [
  {
    path: '/dashboard',
    label: 'Dashboard',
    icon: 'pi-chart-line',
    title: 'Dashboard de inventario',
    subtitle: 'Resumen general del estado del inventario'
  },
  {
    path: '/products',
    label: 'Productos',
    icon: 'pi-box',
    title: 'Productos',
    subtitle: 'Consulta y filtra el catalogo de productos'
  },
  {
    path: '/alerts',
    label: 'Alertas',
    icon: 'pi-bell',
    title: 'Alertas de inventario',
    subtitle: 'Productos con stock bajo o critico'
  },
  {
    path: '/movements',
    label: 'Registrar movimiento',
    icon: 'pi-arrow-right-arrow-left',
    title: 'Registrar movimiento',
    subtitle: 'Registra entradas y salidas de stock'
  }
];

function capitalizeFirst(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}

const TODAY_FORMATTER = new Intl.DateTimeFormat('es-ES', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
  year: 'numeric'
});

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ToastModule],
  templateUrl: './app.html'
})
export class App {
  private readonly router = inject(Router);

  protected readonly navItems = NAV_ITEMS;
  protected readonly today = capitalizeFirst(TODAY_FORMATTER.format(new Date()));

  private readonly currentUrl = signal(this.router.url);

  protected readonly pageTitle = computed(
    () => this.navItems.find((item) => this.currentUrl().startsWith(item.path))?.title ?? 'StockFlow'
  );
  protected readonly pageSubtitle = computed(
    () => this.navItems.find((item) => this.currentUrl().startsWith(item.path))?.subtitle ?? ''
  );

  constructor() {
    this.router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe(() => {
      this.currentUrl.set(this.router.url);
    });
  }
}
