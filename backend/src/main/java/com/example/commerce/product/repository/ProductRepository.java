package com.example.commerce.product.repository;

import com.example.commerce.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 상품 리포지토리. 기본 조회는 삭제된 상품을 제외한다(@SQLRestriction). 코드 중복 검사는 삭제된 상품까지 포함해야 하므로 네이티브 쿼리를 쓴다. */
public interface ProductRepository
    extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

  @Query(value = "select exists(select 1 from products where code = :code)", nativeQuery = true)
  boolean existsByCodeIncludingDeleted(@Param("code") String code);

  @Query(value = "select nextval('product_code_seq')", nativeQuery = true)
  long nextCodeSequence();
}
