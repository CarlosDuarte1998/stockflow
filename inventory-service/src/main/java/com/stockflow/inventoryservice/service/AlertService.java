package com.stockflow.inventoryservice.service;

import com.stockflow.inventoryservice.domain.Product;
import com.stockflow.inventoryservice.domain.StockAlert;
import com.stockflow.inventoryservice.domain.enums.AlertSeverity;
import com.stockflow.inventoryservice.repository.ProductRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    /** Un producto se considera CRITICAL cuando su stock cae a la mitad (o menos) del minimo definido. */
    private static final double CRITICAL_THRESHOLD_RATIO = 0.5;

    private final ProductRepository productRepository;

    public AlertService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @CircuitBreaker(name = "alertsService", fallbackMethod = "getActiveAlertsFallback")
    public List<StockAlert> getActiveAlerts() {
        return productRepository.findProductsBelowMinimumStock().stream()
                .map(this::toStockAlert)
                .toList();
    }

    @SuppressWarnings("unused")
    private List<StockAlert> getActiveAlertsFallback(Throwable throwable) {
        log.warn("Circuit breaker abierto para el servicio de alertas, retornando lista vacia. Causa: {}",
                throwable.getMessage());
        return List.of();
    }

    private StockAlert toStockAlert(Product product) {
        AlertSeverity severity = product.getCurrentStock() <= product.getMinStock() * CRITICAL_THRESHOLD_RATIO
                ? AlertSeverity.CRITICAL
                : AlertSeverity.LOW;

        return StockAlert.builder()
                .productId(product.getId())
                .productName(product.getName())
                .currentStock(product.getCurrentStock())
                .minStock(product.getMinStock())
                .severity(severity)
                .build();
    }
}
