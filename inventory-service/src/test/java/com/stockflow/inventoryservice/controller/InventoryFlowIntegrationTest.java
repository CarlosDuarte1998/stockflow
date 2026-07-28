package com.stockflow.inventoryservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Valida el flujo end-to-end pedido en la prueba tecnica: registrar un movimiento de
 * salida -> actualizar el stock del producto -> disparar una alerta si el stock cae
 * por debajo del minimo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InventoryFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registeringOutMovementUpdatesStockAndTriggersAlert() throws Exception {
        // Producto ELEC-003 (id=3): currentStock=15, minStock=5 segun data.sql
        mockMvc.perform(get("/api/v1/products/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock").value(15));

        Map<String, Object> body = Map.of(
                "productId", 3,
                "type", "OUT",
                "quantity", 12,
                "reason", "Venta a cliente");

        mockMvc.perform(post("/api/v1/movements")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(3))
                .andExpect(jsonPath("$.type").value("OUT"));

        mockMvc.perform(get("/api/v1/products/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock").value(3));

        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.productId == 3)]").exists());

        mockMvc.perform(get("/api/v1/movements/3/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    void registeringOutMovementWithInsufficientStockReturns422() throws Exception {
        Map<String, Object> body = Map.of(
                "productId", 3,
                "type", "OUT",
                "quantity", 9999,
                "reason", "Venta imposible");

        mockMvc.perform(post("/api/v1/movements")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void invalidMovementPayloadReturns400() throws Exception {
        Map<String, Object> body = Map.of("productId", 3, "type", "OUT", "quantity", -5);

        mockMvc.perform(post("/api/v1/movements")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUnknownProductReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/products/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listProductsSupportsPaginationAndCategoryFilter() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(get("/api/v1/products").param("category", "ELECTRONICA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category").value("ELECTRONICA"));
    }
}
