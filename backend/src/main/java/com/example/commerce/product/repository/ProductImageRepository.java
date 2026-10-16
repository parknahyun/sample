package com.example.commerce.product.repository;

import com.example.commerce.product.domain.ProductImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

  int countByProductId(Long productId);

  List<ProductImage> findByProductIdOrderByPositionAsc(Long productId);

  Optional<ProductImage> findByIdAndProductId(Long id, Long productId);

  @Query(
      "select coalesce(max(i.position), -1) + 1 from ProductImage i where i.product.id = :productId")
  int nextPosition(@Param("productId") Long productId);

  /** 삭제되지 않은 상품의 이미지만 찾는다. */
  @Query(
      "select i from ProductImage i join i.product p"
          + " where i.storageKey = :storageKey and p.deletedAt is null")
  Optional<ProductImage> findServable(@Param("storageKey") String storageKey);
}
