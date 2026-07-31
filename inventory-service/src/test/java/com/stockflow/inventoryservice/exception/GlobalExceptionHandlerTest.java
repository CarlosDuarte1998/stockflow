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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Cubre solo los casos que InventoryFlowIntegrationTest NO ejercita por HTTP real
 * (429 y 503 no se disparan facilmente end-to-end, y 500 generico tampoco). Los casos
 * 404/422/400 ya quedan probados de punta a punta en el test de integracion, asi que
 * no se duplican aqui.
 */
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
