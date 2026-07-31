package com.stockflow.inventoryservice.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.HandlerMethod;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest peticion;

    private GlobalExceptionHandler manejador;

    @BeforeEach
    void setUp() {
        manejador = new GlobalExceptionHandler();
        when(peticion.getRequestURI()).thenReturn("/api/v1/products/99");
    }

    @Test
    void manejaProductoNoEncontradoComo404() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejarProductoNoEncontrado(new ProductNotFoundException(99L), peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody().getStatus()).isEqualTo(404);
        assertThat(respuesta.getBody().getPath()).isEqualTo("/api/v1/products/99");
    }

    @Test
    void manejaStockInsuficienteComo422() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejarStockInsuficiente(
                new InsufficientStockException(1L, 3, 10), peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(respuesta.getBody().getMessage()).contains("disponible=3");
    }

    @Test
    void manejaErroresDeValidacionComo400() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("movementRequest", "quantity", "quantity debe ser mayor a 0");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                mock(org.springframework.core.MethodParameter.class), bindingResult);

        ResponseEntity<ErrorResponse> respuesta = manejador.manejarValidacion(ex, peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().getMessage()).contains("quantity debe ser mayor a 0");
    }

    @Test
    void manejaLimiteDePeticionesExcedidoComo429() {
        RateLimiter rateLimiter = RateLimiter.ofDefaults("movementHistory");
        RequestNotPermitted ex = RequestNotPermitted.createRequestNotPermitted(rateLimiter);

        ResponseEntity<ErrorResponse> respuesta = manejador.manejarLimiteExcedido(ex, peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void manejaCircuitoAbiertoComo503() {
        CallNotPermittedException ex = CallNotPermittedException.createCallNotPermittedException(
                io.github.resilience4j.circuitbreaker.CircuitBreaker.ofDefaults("alertsService"));

        ResponseEntity<ErrorResponse> respuesta = manejador.manejarCircuitoAbierto(ex, peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void manejaExcepcionGenericaComo500() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejarGenerico(new RuntimeException("boom"), peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getBody().getMessage()).contains("boom");
    }
}
