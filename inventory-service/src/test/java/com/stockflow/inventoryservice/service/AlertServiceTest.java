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
    void productoExactamenteEnMitadDeMinStockEsCritico() {
        Product producto = productoConStock(5, 10); // 5 <= 10 * 0.5
        when(productRepository.obtenerProductosBajoStockMinimo()).thenReturn(List.of(producto));

        List<StockAlert> alertas = alertService.obtenerAlertasActivas();

        assertThat(alertas).hasSize(1);
        assertThat(alertas.get(0).getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    void productoBajoMinimoPeroSobreMitadEsBajo() {
        Product producto = productoConStock(8, 10); // 8 <= 10 pero > 5 (mitad)
        when(productRepository.obtenerProductosBajoStockMinimo()).thenReturn(List.of(producto));

        List<StockAlert> alertas = alertService.obtenerAlertasActivas();

        assertThat(alertas.get(0).getSeverity()).isEqualTo(AlertSeverity.LOW);
    }

    @Test
    void productoConStockCeroEsCritico() {
        Product producto = productoConStock(0, 6);
        when(productRepository.obtenerProductosBajoStockMinimo()).thenReturn(List.of(producto));

        List<StockAlert> alertas = alertService.obtenerAlertasActivas();

        assertThat(alertas.get(0).getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    void fallbackRetornaListaVaciaCuandoCircuitoEstaAbierto() {
        List<StockAlert> resultadoFallback = invocarFallback(new RuntimeException("circuito abierto"));

        assertThat(resultadoFallback).isEmpty();
    }

    private List<StockAlert> invocarFallback(Throwable causa) {
        try {
            var method = AlertService.class.getDeclaredMethod("obtenerAlertasActivasFallback", Throwable.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<StockAlert> resultado = (List<StockAlert>) method.invoke(alertService, causa);
            return resultado;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private Product productoConStock(int currentStock, int minStock) {
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
