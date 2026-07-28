package com.stockflow.inventoryservice.controller;

import com.stockflow.inventoryservice.dto.MovementRequest;
import com.stockflow.inventoryservice.dto.MovementResponse;
import com.stockflow.inventoryservice.service.MovementService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movements")
@Tag(name = "Movements", description = "Registro y consulta de movimientos de inventario")
public class MovementController {

    private final MovementService movementService;

    public MovementController(MovementService movementService) {
        this.movementService = movementService;
    }

    @PostMapping
    @Operation(summary = "Registrar un movimiento de inventario (actualiza el stock automaticamente)")
    @ApiResponse(responseCode = "201", description = "Movimiento registrado")
    @ApiResponse(responseCode = "422", description = "Stock insuficiente para un movimiento de salida")
    @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos")
    public ResponseEntity<MovementResponse> registerMovement(@Valid @RequestBody MovementRequest request) {
        MovementResponse response = movementService.registerMovement(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{productId}/history")
    @RateLimiter(name = "movementHistory")
    @Operation(summary = "Obtener el historial de movimientos de un producto")
    @ApiResponse(responseCode = "200", description = "Historial de movimientos")
    @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    public ResponseEntity<List<MovementResponse>> getHistory(@PathVariable Long productId) {
        return ResponseEntity.ok(movementService.getHistory(productId));
    }
}
