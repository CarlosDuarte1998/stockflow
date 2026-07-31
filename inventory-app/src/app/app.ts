import { Component, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ToastModule } from 'primeng/toast';
import { filter } from 'rxjs';

interface ItemNavegacion {
  path: string;
  etiqueta: string;
  icono: string;
  titulo: string;
  subtitulo: string;
}

const ITEMS_NAVEGACION: ItemNavegacion[] = [
  {
    path: '/dashboard',
    etiqueta: 'Dashboard',
    icono: 'pi-chart-line',
    titulo: 'Dashboard de inventario',
    subtitulo: 'Resumen general del estado del inventario'
  },
  {
    path: '/products',
    etiqueta: 'Productos',
    icono: 'pi-box',
    titulo: 'Productos',
    subtitulo: 'Consulta y filtra el catalogo de productos'
  },
  {
    path: '/alerts',
    etiqueta: 'Alertas',
    icono: 'pi-bell',
    titulo: 'Alertas de inventario',
    subtitulo: 'Productos con stock bajo o critico'
  },
  {
    path: '/movements',
    etiqueta: 'Registrar movimiento',
    icono: 'pi-arrow-right-arrow-left',
    titulo: 'Registrar movimiento',
    subtitulo: 'Registra entradas y salidas de stock'
  }
];

function capitalizarPrimeraLetra(texto: string): string {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

const FORMATEADOR_FECHA = new Intl.DateTimeFormat('es-ES', {
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

  protected readonly itemsNavegacion = ITEMS_NAVEGACION;
  protected readonly hoy = capitalizarPrimeraLetra(FORMATEADOR_FECHA.format(new Date()));

  private readonly urlActual = signal(this.router.url);

  protected readonly tituloPagina = computed(
    () => this.itemsNavegacion.find((item) => this.urlActual().startsWith(item.path))?.titulo ?? 'StockFlow'
  );
  protected readonly subtituloPagina = computed(
    () => this.itemsNavegacion.find((item) => this.urlActual().startsWith(item.path))?.subtitulo ?? ''
  );

  constructor() {
    this.router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe(() => {
      this.urlActual.set(this.router.url);
    });
  }
}
