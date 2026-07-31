package com.stockflow.inventoryservice.service;

import com.stockflow.inventoryservice.domain.Movement;
import com.stockflow.inventoryservice.domain.Product;
import com.stockflow.inventoryservice.domain.enums.MovementType;
import com.stockflow.inventoryservice.dto.MovementRequest;
import com.stockflow.inventoryservice.dto.MovementResponse;
import com.stockflow.inventoryservice.exception.InsufficientStockException;
import com.stockflow.inventoryservice.repository.MovementRepository;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class MovementService {

    private final MovementRepository movementRepository;
    private final ProductService productService;

    public MovementService(MovementRepository movementRepository, ProductService productService) {
        this.movementRepository = movementRepository;
        this.productService = productService;
    }

    /**
     * Flujo principal: registrar movimiento -> actualizar stock del producto.
     * Las alertas se recalculan en tiempo real por AlertService al consultar GET /api/v1/alerts,
     * por lo que un OUT que deje currentStock &lt;= minStock queda reflejado de inmediato.
     */
    @Retry(name = "movementRegistration")
    @Transactional
    public MovementResponse registrarMovimiento(MovementRequest solicitud) {
        Product producto = productService.obtenerProductoEntidad(solicitud.getProductId());

        if (solicitud.getType() == MovementType.OUT && producto.getCurrentStock() < solicitud.getQuantity()) {
            throw new InsufficientStockException(producto.getId(), producto.getCurrentStock(), solicitud.getQuantity());
        }

        int variacion = solicitud.getType() == MovementType.IN ? solicitud.getQuantity() : -solicitud.getQuantity();
        producto.setCurrentStock(producto.getCurrentStock() + variacion);

        Movement movimiento = Movement.builder()
                .productId(producto.getId())
                .type(solicitud.getType())
                .quantity(solicitud.getQuantity())
                .reason(solicitud.getReason())
                .timestamp(Instant.now())
                .build();

        Movement guardado = movementRepository.save(movimiento);
        return MovementResponse.desdeEntidad(guardado);
    }

    public List<MovementResponse> obtenerHistorial(Long idProducto) {
        productService.obtenerProductoEntidad(idProducto);
        return movementRepository.obtenerHistorialPorProducto(idProducto).stream()
                .map(MovementResponse::desdeEntidad)
                .toList();
    }
}
