package com.example.commerce.audit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductChangeLogRepository extends JpaRepository<ProductChangeLog, Long> {

  List<ProductChangeLog> findByProductIdOrderByIdAsc(Long productId);
}
