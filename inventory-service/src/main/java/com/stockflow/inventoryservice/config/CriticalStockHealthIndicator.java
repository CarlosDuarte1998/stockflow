package com.stockflow.inventoryservice.config;

import com.stockflow.inventoryservice.domain.Product;
import com.stockflow.inventoryservice.repository.ProductRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reporta DOWN cuando mas del 20% de los productos estan en estado de alerta critica
 * (currentStock &lt;= 50% de minStock), para que el operador detecte una situacion de
 * inventario generalizada antes de que impacte al negocio.
 */
@Component("criticalStock")
public class CriticalStockHealthIndicator implements HealthIndicator {

    private static final double CRITICAL_THRESHOLD_RATIO = 0.5;
    private static final double UNHEALTHY_PRODUCT_PERCENTAGE = 20.0;

    private final ProductRepository productRepository;

    public CriticalStockHealthIndicator(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Health health() {
        List<Product> products = productRepository.findAll();

        if (products.isEmpty()) {
            return Health.unknown().withDetail("reason", "No hay productos registrados").build();
        }

        long criticalCount = products.stream()
                .filter(p -> p.getCurrentStock() <= p.getMinStock() * CRITICAL_THRESHOLD_RATIO)
                .count();

        double percentage = (criticalCount * 100.0) / products.size();

        Health.Builder builder = percentage > UNHEALTHY_PRODUCT_PERCENTAGE ? Health.down() : Health.up();

        return builder
                .withDetail("criticalProducts", criticalCount)
                .withDetail("totalProducts", products.size())
                .withDetail("criticalPercentage", String.format("%.2f%%", percentage))
                .withDetail("threshold", UNHEALTHY_PRODUCT_PERCENTAGE + "%")
                .build();
    }
}
