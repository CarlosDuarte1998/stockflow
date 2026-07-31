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
        List<Product> productos = productRepository.findAll();

        if (productos.isEmpty()) {
            return Health.unknown().withDetail("reason", "No hay productos registrados").build();
        }

        long conteoCritico = productos.stream()
                .filter(p -> p.getCurrentStock() <= p.getMinStock() * CRITICAL_THRESHOLD_RATIO)
                .count();

        double porcentaje = (conteoCritico * 100.0) / productos.size();

        Health.Builder constructorSalud = porcentaje > UNHEALTHY_PRODUCT_PERCENTAGE ? Health.down() : Health.up();

        return constructorSalud
                .withDetail("criticalProducts", conteoCritico)
                .withDetail("totalProducts", productos.size())
                .withDetail("criticalPercentage", String.format("%.2f%%", porcentaje))
                .withDetail("threshold", UNHEALTHY_PRODUCT_PERCENTAGE + "%")
                .build();
    }
}
