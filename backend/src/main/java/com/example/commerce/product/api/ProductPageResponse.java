package com.example.commerce.product.api;

import java.util.List;
import org.springframework.data.domain.Page;

/** 페이지 단위 상품 목록. 결과가 없으면 content가 빈 배열이다. */
public record ProductPageResponse(
    List<ProductSummaryResponse> content, int page, int size, long totalElements, int totalPages) {

  public static ProductPageResponse of(Page<ProductSummaryResponse> page) {
    return new ProductPageResponse(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
