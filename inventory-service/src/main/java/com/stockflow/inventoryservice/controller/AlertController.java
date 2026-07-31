package com.stockflow.inventoryservice.controller;

import com.stockflow.inventoryservice.domain.StockAlert;
import com.stockflow.inventoryservice.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Alertas de productos con stock bajo o critico")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @Operation(summary = "Listar productos con stock actual por debajo o igual al minimo definido")
    @ApiResponse(responseCode = "200", description = "Alertas activas")
    public ResponseEntity<List<StockAlert>> obtenerAlertas() {
        return ResponseEntity.ok(alertService.obtenerAlertasActivas());
    }
}
