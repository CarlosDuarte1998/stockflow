package com.stockflow.inventoryservice.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long idProducto, int disponible, int solicitado) {
        super("Stock insuficiente para el producto " + idProducto
                + ": disponible=" + disponible + ", solicitado=" + solicitado);
    }
}
