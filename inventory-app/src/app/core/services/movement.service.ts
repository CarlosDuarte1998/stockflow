import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Movement, MovementRequest } from '../models/movement.model';

@Injectable({ providedIn: 'root' })
export class MovementService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/movements`;

  registrar(solicitud: MovementRequest): Observable<Movement> {
    return this.http.post<Movement>(this.baseUrl, solicitud);
  }

  historial(idProducto: number): Observable<Movement[]> {
    return this.http.get<Movement[]>(`${this.baseUrl}/${idProducto}/history`);
  }
}
