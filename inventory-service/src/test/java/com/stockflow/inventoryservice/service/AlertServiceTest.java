package com.stockflow.inventoryservice.service;

import com.stockflow.inventoryservice.domain.Product;
import com.stockflow.inventoryservice.domain.StockAlert;
import com.stockflow.inventoryservice.domain.enums.AlertSeverity;
import com.stockflow.inventoryservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private AlertService alertService;

    @Test
    void productAtExactlyHalfOfMinStockIsCritical() {
        Product product = productWithStock(5, 10); // 5 <= 10 * 0.5
        when(productRepository.findProductsBelowMinimumStock()).thenReturn(List.of(product));

        List<StockAlert> alerts = alertService.getActiveAlerts();

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    void productBelowMinButAboveHalfIsLow() {
        Product product = productWithStock(8, 10); // 8 <= 10 pero > 5 (mitad)
        when(productRepository.findProductsBelowMinimumStock()).thenReturn(List.of(product));

        List<StockAlert> alerts = alertService.getActiveAlerts();

        assertThat(alerts.get(0).getSeverity()).isEqualTo(AlertSeverity.LOW);
    }

    @Test
    void productWithZeroStockIsCritical() {
        Product product = productWithStock(0, 6);
        when(productRepository.findProductsBelowMinimumStock()).thenReturn(List.of(product));

        List<StockAlert> alerts = alertService.getActiveAlerts();

        assertThat(alerts.get(0).getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    void fallbackReturnsEmptyListWhenCircuitIsOpen() {
        List<StockAlert> fallbackResult = invokeFallback(new RuntimeException("circuito abierto"));

        assertThat(fallbackResult).isEmpty();
    }

    private List<StockAlert> invokeFallback(Throwable throwable) {
        try {
            var method = AlertService.class.getDeclaredMethod("getActiveAlertsFallback", Throwable.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<StockAlert> result = (List<StockAlert>) method.invoke(alertService, throwable);
            return result;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private Product productWithStock(int currentStock, int minStock) {
        return Product.builder()
                .id(1L)
                .sku("SKU-1")
                .name("Producto de prueba")
                .category("TEST")
                .currentStock(currentStock)
                .minStock(minStock)
                .unitPrice(BigDecimal.TEN)
                .build();
    }
}
