import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MessageService } from 'primeng/api';
import { catchError, throwError } from 'rxjs';

import { ErrorResponse } from '../models/error-response.model';

/**
 * Parsea el ErrorResponse consistente que retorna inventory-service
 * (timestamp, status, error, message, path) y lo muestra como toast.
 */
export const interceptorErrores: HttpInterceptorFn = (peticion, siguiente) => {
  const messageService = inject(MessageService);

  return siguiente(peticion).pipe(
    catchError((error: HttpErrorResponse) => {
      const cuerpo = error.error as ErrorResponse | undefined;
      const detalle = cuerpo?.message ?? error.message ?? 'Ocurrio un error inesperado';

      messageService.add({
        severity: 'error',
        summary: cuerpo?.error ?? `Error ${error.status}`,
        detail: detalle,
        life: 5000
      });

      return throwError(() => error);
    })
  );
};
