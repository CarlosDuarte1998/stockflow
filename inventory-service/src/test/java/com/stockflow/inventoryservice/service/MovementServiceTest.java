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
import org.mockito.ArgumentCaptor;
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

    private Product product;

    @BeforeEach
    void setUp() {
        movementService = new MovementService(movementRepository, productService);
        product = Product.builder()
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
    void registerMovementInIncreasesStock() {
        when(productService.getProductEntity(1L)).thenReturn(product);
        when(movementRepository.save(any(Movement.class))).thenAnswer(invocation -> {
            Movement m = invocation.getArgument(0);
            m.setId(1L);
            return m;
        });

        MovementRequest request = new MovementRequest(1L, MovementType.IN, 10, "Reabastecimiento");
        MovementResponse response = movementService.registerMovement(request);

        assertThat(product.getCurrentStock()).isEqualTo(25);
        assertThat(response.getType()).isEqualTo(MovementType.IN);
        assertThat(response.getQuantity()).isEqualTo(10);
    }

    @Test
    void registerMovementOutDecreasesStockAndCanTriggerAlertCondition() {
        when(productService.getProductEntity(1L)).thenReturn(product);
        when(movementRepository.save(any(Movement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovementRequest request = new MovementRequest(1L, MovementType.OUT, 12, "Venta");
        movementService.registerMovement(request);

        // 15 - 12 = 3, que es <= minStock (5): el flujo de negocio principal a validar.
        assertThat(product.getCurrentStock()).isEqualTo(3);
        assertThat(product.isBelowMinimum()).isTrue();
    }

    @Test
    void registerMovementOutWithInsufficientStockThrows() {
        when(productService.getProductEntity(1L)).thenReturn(product);

        MovementRequest request = new MovementRequest(1L, MovementType.OUT, 999, "Venta");

        assertThatThrownBy(() -> movementService.registerMovement(request))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void registerMovementPersistsMovementWithTimestamp() {
        when(productService.getProductEntity(1L)).thenReturn(product);
        ArgumentCaptor<Movement> captor = ArgumentCaptor.forClass(Movement.class);
        when(movementRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        movementService.registerMovement(new MovementRequest(1L, MovementType.IN, 5, "Ajuste"));

        Movement saved = captor.getValue();
        assertThat(saved.getProductId()).isEqualTo(1L);
        assertThat(saved.getTimestamp()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void getHistoryThrowsWhenProductNotFound() {
        when(productService.getProductEntity(99L)).thenThrow(new ProductNotFoundException(99L));

        assertThatThrownBy(() -> movementService.getHistory(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void getHistoryReturnsMovementsOrderedByTimestampDesc() {
        when(productService.getProductEntity(1L)).thenReturn(product);
        Movement movement = Movement.builder()
                .id(1L).productId(1L).type(MovementType.OUT).quantity(2).reason("x").timestamp(Instant.now())
                .build();
        when(movementRepository.findByProductIdOrderByTimestampDesc(1L)).thenReturn(List.of(movement));

        List<MovementResponse> history = movementService.getHistory(1L);

        assertThat(history).hasSize(1);
        verify(movementRepository).findByProductIdOrderByTimestampDesc(1L);
    }
}
