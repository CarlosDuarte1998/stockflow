package com.stockflow.inventoryservice.dto;

import com.stockflow.inventoryservice.domain.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Representacion de un producto del inventario")
public class ProductResponse {

    @Schema(example = "1")
    private Long id;
    @Schema(example = "ELEC-001")
    private String sku;
    @Schema(example = "Mouse inalambrico")
    private String name;
    @Schema(example = "ELECTRONICA")
    private String category;
    @Schema(example = "45")
    private Integer currentStock;
    @Schema(example = "10")
    private Integer minStock;
    @Schema(example = "12.99")
    private BigDecimal unitPrice;

    public static ProductResponse fromEntity(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .category(product.getCategory())
                .currentStock(product.getCurrentStock())
                .minStock(product.getMinStock())
                .unitPrice(product.getUnitPrice())
                .build();
    }
}
