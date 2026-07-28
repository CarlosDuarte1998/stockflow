package com.stockflow.inventoryservice.dto;

import com.stockflow.inventoryservice.domain.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String sku;
    private String name;
    private String category;
    private Integer currentStock;
    private Integer minStock;
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
