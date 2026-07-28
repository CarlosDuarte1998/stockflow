package com.stockflow.inventoryservice.domain;

import com.stockflow.inventoryservice.domain.enums.AlertSeverity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Modelo de solo lectura calculado a partir de Product; no se persiste.
 * Severity: CRITICAL si currentStock &lt;= 50% de minStock (o es 0), LOW en el resto de casos
 * en los que currentStock &lt;= minStock.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Alerta de stock bajo o critico para un producto")
public class StockAlert {

    private Long productId;
    private String productName;
    private Integer currentStock;
    private Integer minStock;
    private AlertSeverity severity;
}
