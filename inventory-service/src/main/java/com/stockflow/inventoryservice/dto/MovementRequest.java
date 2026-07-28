package com.stockflow.inventoryservice.dto;

import com.stockflow.inventoryservice.domain.enums.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos requeridos para registrar un movimiento de inventario")
public class MovementRequest {

    @NotNull(message = "productId es obligatorio")
    @Schema(description = "Identificador del producto afectado", example = "1")
    private Long productId;

    @NotNull(message = "type es obligatorio")
    @Schema(description = "Tipo de movimiento", example = "OUT")
    private MovementType type;

    @NotNull(message = "quantity es obligatorio")
    @Min(value = 1, message = "quantity debe ser mayor a 0")
    @Schema(description = "Cantidad de unidades del movimiento", example = "5")
    private Integer quantity;

    @Size(max = 255, message = "reason no puede superar los 255 caracteres")
    @Schema(description = "Motivo del movimiento", example = "Venta a cliente")
    private String reason;
}
