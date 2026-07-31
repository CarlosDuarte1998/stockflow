package com.stockflow.inventoryservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Respuesta paginada con solo los campos necesarios para paginar en el cliente")
public record PagedResponse<T>(
        List<T> content,
        @Schema(example = "0") int number,
        @Schema(example = "10") int size,
        @Schema(example = "12") long totalElements,
        @Schema(example = "2") int totalPages
) {

    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
