package com.stockflow.inventoryservice.dto;

import com.stockflow.inventoryservice.domain.Movement;
import com.stockflow.inventoryservice.domain.enums.MovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementResponse {

    private Long id;
    private Long productId;
    private MovementType type;
    private Integer quantity;
    private String reason;
    private Instant timestamp;

    public static MovementResponse fromEntity(Movement movement) {
        return MovementResponse.builder()
                .id(movement.getId())
                .productId(movement.getProductId())
                .type(movement.getType())
                .quantity(movement.getQuantity())
                .reason(movement.getReason())
                .timestamp(movement.getTimestamp())
                .build();
    }
}
