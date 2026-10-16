package com.example.commerce.storefront;

import java.util.List;
import org.springframework.data.domain.Page;

/** 고객 노출용 상품 목록 페이지. */
public record StorefrontProductPageResponse(
    List<StorefrontProductResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages) {

  static StorefrontProductPageResponse of(Page<StorefrontProductResponse> page) {
    return new StorefrontProductPageResponse(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
