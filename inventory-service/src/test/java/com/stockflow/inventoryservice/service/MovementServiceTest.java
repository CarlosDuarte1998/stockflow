package com.stockflow.inventoryservice.service;

import com.stockflow.inventoryservice.domain.Movement;
import com.stockflow.inventoryservice.domain.Product;
import com.stockflow.inventoryservice.domain.enums.MovementType;
import com.stockflow.inventoryservice.dto.MovementRequest;
import com.stockflow.inventoryservice.dto.MovementResponse;
import com.stockflow.inventoryservice.exception.InsufficientStockException;
import com.stockflow.inventoryservice.exception.ProductNotFoundException;
import com.stockflow.inventoryservice.repository.MovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovementServiceTest {

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private ProductService productService;

    private MovementService movementService;

    private Product producto;

    @BeforeEach
    void setUp() {
        movementService = new MovementService(movementRepository, productService);
        producto = Product.builder()
                .id(1L)
                .sku("ELEC-003")
                .name("Monitor 24\"")
                .category("ELECTRONICA")
                .currentStock(15)
                .minStock(5)
                .unitPrice(new BigDecimal("159.00"))
                .build();
    }

    @Test
    void registrarMovimientoInIncrementaStock() {
        when(productService.obtenerProductoEntidad(1L)).thenReturn(producto);
        when(movementRepository.save(any(Movement.class))).thenAnswer(invocation -> {
            Movement m = invocation.getArgument(0);
            m.setId(1L);
            return m;
        });

        MovementRequest solicitud = new MovementRequest(1L, MovementType.IN, 10, "Reabastecimiento");
        MovementResponse respuesta = movementService.registrarMovimiento(solicitud);

        assertThat(producto.getCurrentStock()).isEqualTo(25);
        assertThat(respuesta.getType()).isEqualTo(MovementType.IN);
        assertThat(respuesta.getQuantity()).isEqualTo(10);
    }

    @Test
    void registrarMovimientoOutDisminuyeStockYPuedeDispararAlerta() {
        when(productService.obtenerProductoEntidad(1L)).thenReturn(producto);
        when(movementRepository.save(any(Movement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovementRequest solicitud = new MovementRequest(1L, MovementType.OUT, 12, "Venta");
        movementService.registrarMovimiento(solicitud);

        // 15 - 12 = 3, que es <= minStock (5): el flujo de negocio principal a validar.
        assertThat(producto.getCurrentStock()).isEqualTo(3);
        assertThat(producto.isBelowMinimum()).isTrue();
    }

    @Test
    void registrarMovimientoOutConStockInsuficienteLanzaExcepcion() {
        when(productService.obtenerProductoEntidad(1L)).thenReturn(producto);

        MovementRequest solicitud = new MovementRequest(1L, MovementType.OUT, 999, "Venta");

        assertThatThrownBy(() -> movementService.registrarMovimiento(solicitud))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void obtenerHistorialLanzaExcepcionCuandoProductoNoExiste() {
        when(productService.obtenerProductoEntidad(99L)).thenThrow(new ProductNotFoundException(99L));

        assertThatThrownBy(() -> movementService.obtenerHistorial(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void obtenerHistorialRetornaMovimientosOrdenadosPorTimestampDesc() {
        when(productService.obtenerProductoEntidad(1L)).thenReturn(producto);
        Movement movimiento = Movement.builder()
                .id(1L).productId(1L).type(MovementType.OUT).quantity(2).reason("x").timestamp(Instant.now())
                .build();
        when(movementRepository.obtenerHistorialPorProducto(1L)).thenReturn(List.of(movimiento));

        List<MovementResponse> historial = movementService.obtenerHistorial(1L);

        assertThat(historial).hasSize(1);
        verify(movementRepository).obtenerHistorialPorProducto(1L);
    }
}
