import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MessageService } from 'primeng/api';
import { catchError, throwError } from 'rxjs';

import { ErrorResponse } from '../models/error-response.model';

/**
 * Parsea el ErrorResponse consistente que retorna inventory-service
 * (timestamp, status, error, message, path) y lo muestra como toast.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const messageService = inject(MessageService);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      const body = err.error as ErrorResponse | undefined;
      const detail = body?.message ?? err.message ?? 'Ocurrio un error inesperado';

      messageService.add({
        severity: 'error',
        summary: body?.error ?? `Error ${err.status}`,
        detail,
        life: 5000
      });

      return throwError(() => err);
    })
  );
};
