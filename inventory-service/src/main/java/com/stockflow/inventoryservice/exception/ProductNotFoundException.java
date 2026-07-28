package com.stockflow.inventoryservice.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("Producto no encontrado con id: " + productId);
    }
}
