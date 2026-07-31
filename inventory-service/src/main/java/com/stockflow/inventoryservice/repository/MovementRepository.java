package com.stockflow.inventoryservice.repository;

import com.stockflow.inventoryservice.domain.Movement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovementRepository extends JpaRepository<Movement, Long> {

    @Query("SELECT m FROM Movement m WHERE m.productId = :idProducto ORDER BY m.timestamp DESC")
    List<Movement> obtenerHistorialPorProducto(@Param("idProducto") Long idProducto);
}
