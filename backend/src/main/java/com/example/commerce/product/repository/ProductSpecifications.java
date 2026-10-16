package com.example.commerce.product.repository;

import com.example.commerce.product.domain.Product;
import com.example.commerce.product.domain.ProductStatus;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;

/** 상품 목록 검색 조건(FR-010). 조건이 없으면 모든 상품(삭제되지 않은 것)이 대상이다. */
public final class ProductSpecifications {

  private static final char ESCAPE = '\\';

  private ProductSpecifications() {}

  public static Specification<Product> search(String q, Long categoryId, ProductStatus status) {
    Specification<Product> spec = (root, query, cb) -> cb.conjunction();
    if (q != null && !q.isBlank()) {
      spec = spec.and(nameContains(q.trim()));
    }
    if (categoryId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
    }
    if (status != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }
    return spec;
  }

  /** 상품명 부분 일치(대소문자 무시). 검색어의 와일드카드 문자(%, _)는 문자 그대로 찾는다. */
  static Specification<Product> nameContains(String q) {
    String pattern = "%" + escapeLike(q) + "%";
    return (root, query, cb) ->
        ((HibernateCriteriaBuilder) cb).ilike(root.<String>get("name"), pattern, ESCAPE);
  }

  static String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
