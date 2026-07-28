package com.stockflow.inventoryservice.repository;

import com.stockflow.inventoryservice.domain.Movement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovementRepository extends JpaRepository<Movement, Long> {

    List<Movement> findByProductIdOrderByTimestampDesc(Long productId);
}
