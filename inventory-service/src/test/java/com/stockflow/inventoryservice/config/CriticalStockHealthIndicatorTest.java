package com.stockflow.inventoryservice.config;

import com.stockflow.inventoryservice.domain.Product;
import com.stockflow.inventoryservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CriticalStockHealthIndicatorTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CriticalStockHealthIndicator healthIndicator;

    @Test
    void reportaDownCuandoMasDe20PorcientoEsCritico() {
        // 2 de 5 productos criticos = 40% > 20%
        when(productRepository.findAll()).thenReturn(List.of(
                critico(), critico(), saludable(), saludable(), saludable()));

        Health salud = healthIndicator.health();

        assertThat(salud.getStatus()).isEqualTo(Status.DOWN);
        assertThat(salud.getDetails()).containsEntry("criticalProducts", 2L);
    }

    @Test
    void reportaUpCuando20PorcientoOMenosEsCritico() {
        // 1 de 5 = 20%, no supera el umbral (estrictamente mayor a 20%)
        when(productRepository.findAll()).thenReturn(List.of(
                critico(), saludable(), saludable(), saludable(), saludable()));

        Health salud = healthIndicator.health();

        assertThat(salud.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void reportaUnknownCuandoNoHayProductos() {
        when(productRepository.findAll()).thenReturn(List.of());

        Health salud = healthIndicator.health();

        assertThat(salud.getStatus()).isEqualTo(Status.UNKNOWN);
    }

    private Product critico() {
        return Product.builder().id(1L).sku("A").name("A").category("C")
                .currentStock(0).minStock(10).unitPrice(BigDecimal.ONE).build();
    }

    private Product saludable() {
        return Product.builder().id(2L).sku("B").name("B").category("C")
                .currentStock(50).minStock(10).unitPrice(BigDecimal.ONE).build();
    }
}
