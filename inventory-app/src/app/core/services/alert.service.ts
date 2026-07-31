import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { StockAlert } from '../models/alert.model';

@Injectable({ providedIn: 'root' })
export class AlertService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/alerts`;

  listar(): Observable<StockAlert[]> {
    return this.http.get<StockAlert[]>(this.baseUrl);
  }
}
