package com.stockflow.inventoryservice.repository;

import com.stockflow.inventoryservice.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE LOWER(p.category) = LOWER(:categoria)")
    Page<Product> buscarPorCategoria(@Param("categoria") String categoria, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.currentStock <= p.minStock")
    List<Product> obtenerProductosBajoStockMinimo();
}
