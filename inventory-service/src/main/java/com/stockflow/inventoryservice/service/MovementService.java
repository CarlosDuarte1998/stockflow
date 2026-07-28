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
    public MovementResponse registerMovement(MovementRequest request) {
        Product product = productService.getProductEntity(request.getProductId());

        if (request.getType() == MovementType.OUT && product.getCurrentStock() < request.getQuantity()) {
            throw new InsufficientStockException(product.getId(), product.getCurrentStock(), request.getQuantity());
        }

        int delta = request.getType() == MovementType.IN ? request.getQuantity() : -request.getQuantity();
        product.setCurrentStock(product.getCurrentStock() + delta);

        Movement movement = Movement.builder()
                .productId(product.getId())
                .type(request.getType())
                .quantity(request.getQuantity())
                .reason(request.getReason())
                .timestamp(Instant.now())
                .build();

        Movement saved = movementRepository.save(movement);
        return MovementResponse.fromEntity(saved);
    }

    public List<MovementResponse> getHistory(Long productId) {
        productService.getProductEntity(productId);
        return movementRepository.findByProductIdOrderByTimestampDesc(productId).stream()
                .map(MovementResponse::fromEntity)
                .toList();
    }
}
