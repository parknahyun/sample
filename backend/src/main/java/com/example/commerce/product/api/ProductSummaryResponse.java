package com.example.commerce.product.api;

import com.example.commerce.product.domain.ProductStatus;
import java.time.Instant;

/** 운영자용 상품 목록 항목. */
public record ProductSummaryResponse(
    Long id,
    String code,
    String name,
    long price,
    Long categoryId,
    ProductStatus status,
    long totalStock,
    String primaryImageUrl,
    Instant createdAt) {}
