package com.stockflow.inventoryservice.repository;

import com.stockflow.inventoryservice.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByCategoryIgnoreCase(String category, Pageable pageable);

    Optional<Product> findBySku(String sku);

    @Query("SELECT p FROM Product p WHERE p.currentStock <= p.minStock")
    List<Product> findProductsBelowMinimumStock();
}
