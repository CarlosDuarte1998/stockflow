package com.stockflow.inventoryservice.controller;

import com.stockflow.inventoryservice.dto.PagedResponse;
import com.stockflow.inventoryservice.dto.ProductResponse;
import com.stockflow.inventoryservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Consulta de productos del inventario")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Listar productos con paginacion y filtro opcional por categoria")
    @ApiResponse(responseCode = "200", description = "Pagina de productos")
    public ResponseEntity<PagedResponse<ProductResponse>> listProducts(
            @Parameter(description = "Filtro opcional por categoria") @RequestParam(required = false) String category,
            Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(productService.listProducts(category, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener el detalle de un producto por id")
    @ApiResponse(responseCode = "200", description = "Producto encontrado")
    @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }
}
