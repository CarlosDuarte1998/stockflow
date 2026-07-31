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
    private HttpServletRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/v1/products/99");
    }

    @Test
    void handlesProductNotFoundAs404() {
        ResponseEntity<ErrorResponse> response = handler.manejarProductoNoEncontrado(new ProductNotFoundException(99L), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/products/99");
    }

    @Test
    void handlesInsufficientStockAs422() {
        ResponseEntity<ErrorResponse> response = handler.manejarStockInsuficiente(
                new InsufficientStockException(1L, 3, 10), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().getMessage()).contains("disponible=3");
    }

    @Test
    void handlesValidationErrorsAs400() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("movementRequest", "quantity", "quantity debe ser mayor a 0");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                mock(org.springframework.core.MethodParameter.class), bindingResult);

        ResponseEntity<ErrorResponse> response = handler.manejarValidacion(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("quantity debe ser mayor a 0");
    }

    @Test
    void handlesRateLimitExceededAs429() {
        RateLimiter rateLimiter = RateLimiter.ofDefaults("movementHistory");
        RequestNotPermitted ex = RequestNotPermitted.createRequestNotPermitted(rateLimiter);

        ResponseEntity<ErrorResponse> response = handler.manejarLimiteExcedido(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void handlesCircuitBreakerOpenAs503() {
        CallNotPermittedException ex = CallNotPermittedException.createCallNotPermittedException(
                io.github.resilience4j.circuitbreaker.CircuitBreaker.ofDefaults("alertsService"));

        ResponseEntity<ErrorResponse> response = handler.manejarCircuitoAbierto(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void handlesGenericExceptionAs500() {
        ResponseEntity<ErrorResponse> response = handler.manejarGenerico(new RuntimeException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).contains("boom");
    }
}
