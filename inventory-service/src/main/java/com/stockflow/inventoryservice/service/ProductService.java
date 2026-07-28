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

    public Page<ProductResponse> listProducts(String category, Pageable pageable) {
        Page<Product> products = (category == null || category.isBlank())
                ? productRepository.findAll(pageable)
                : productRepository.findByCategoryIgnoreCase(category, pageable);
        return products.map(ProductResponse::fromEntity);
    }

    public ProductResponse getProduct(Long id) {
        return productRepository.findById(id)
                .map(ProductResponse::fromEntity)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    Product getProductEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
