package com.stockflow.inventoryservice.service;

import com.stockflow.inventoryservice.domain.Product;
import com.stockflow.inventoryservice.dto.ProductResponse;
import com.stockflow.inventoryservice.exception.ProductNotFoundException;
import com.stockflow.inventoryservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product producto;

    @BeforeEach
    void setUp() {
        producto = Product.builder()
                .id(1L)
                .sku("ELEC-001")
                .name("Mouse inalambrico")
                .category("ELECTRONICA")
                .currentStock(45)
                .minStock(10)
                .unitPrice(new BigDecimal("12.99"))
                .build();
    }

    @Test
    void listarProductosSinCategoriaUsaFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(producto)));

        Page<ProductResponse> resultado = productService.listarProductos(null, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getSku()).isEqualTo("ELEC-001");
    }

    @Test
    void listarProductosConCategoriaFiltraPorCategoria() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.buscarPorCategoria(anyString(), any())).thenReturn(new PageImpl<>(List.of(producto)));

        Page<ProductResponse> resultado = productService.listarProductos("ELECTRONICA", pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(productRepository).buscarPorCategoria("ELECTRONICA", pageable);
    }

    @Test
    void obtenerProductoRetornaRespuestaCuandoExiste() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(producto));

        ProductResponse respuesta = productService.obtenerProducto(1L);

        assertThat(respuesta.getId()).isEqualTo(1L);
        assertThat(respuesta.getCurrentStock()).isEqualTo(45);
    }

    @Test
    void obtenerProductoLanzaExcepcionCuandoNoExiste() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.obtenerProducto(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void obtenerProductoEntidadLanzaExcepcionCuandoNoExiste() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.obtenerProductoEntidad(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
