import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Page } from '../models/page.model';
import { Product } from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/products`;

  listar(pagina: number, tamano: number, categoria?: string): Observable<Page<Product>> {
    let params = new HttpParams().set('page', pagina).set('size', tamano);
    if (categoria) {
      params = params.set('category', categoria);
    }
    return this.http.get<Page<Product>>(this.baseUrl, { params });
  }

  obtenerPorId(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.baseUrl}/${id}`);
  }
}
