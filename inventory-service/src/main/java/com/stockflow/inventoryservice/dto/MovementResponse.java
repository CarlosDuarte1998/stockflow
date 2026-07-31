package com.stockflow.inventoryservice.dto;

import com.stockflow.inventoryservice.domain.Movement;
import com.stockflow.inventoryservice.domain.enums.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Movimiento de inventario registrado")
public class MovementResponse {

    private Long id;
    private Long productId;
    private MovementType type;
    private Integer quantity;
    private String reason;
    private Instant timestamp;

    public static MovementResponse desdeEntidad(Movement movimiento) {
        return MovementResponse.builder()
                .id(movimiento.getId())
                .productId(movimiento.getProductId())
                .type(movimiento.getType())
                .quantity(movimiento.getQuantity())
                .reason(movimiento.getReason())
                .timestamp(movimiento.getTimestamp())
                .build();
    }
}
