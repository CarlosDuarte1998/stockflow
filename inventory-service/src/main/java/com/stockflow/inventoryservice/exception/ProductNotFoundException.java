package com.stockflow.inventoryservice.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long idProducto) {
        super("Producto no encontrado con id: " + idProducto);
    }
}
