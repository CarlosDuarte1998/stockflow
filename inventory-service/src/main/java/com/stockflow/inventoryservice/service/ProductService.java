package com.stockflow.inventoryservice.service;

import com.stockflow.inventoryservice.domain.Product;
import com.stockflow.inventoryservice.dto.ProductResponse;
import com.stockflow.inventoryservice.exception.ProductNotFoundException;
import com.stockflow.inventoryservice.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<ProductResponse> listarProductos(String categoria, Pageable pageable) {
        Page<Product> productos = (categoria == null || categoria.isBlank())
                ? productRepository.findAll(pageable)
                : productRepository.buscarPorCategoria(categoria, pageable);
        return productos.map(ProductResponse::desdeEntidad);
    }

    public ProductResponse obtenerProducto(Long id) {
        return productRepository.findById(id)
                .map(ProductResponse::desdeEntidad)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    Product obtenerProductoEntidad(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
