package com.example.commerce.product.api;

import com.example.commerce.product.domain.ProductStatus;
import java.time.Instant;
import java.util.List;

/** 운영자용 상품 상세. 수정 시 version을 함께 보내야 한다. */
public record ProductResponse(
    Long id,
    String code,
    String name,
    String description,
    long price,
    Long categoryId,
    ProductStatus status,
    Integer stockQuantity,
    List<OptionGroupDto> optionGroups,
    List<VariantDto> variants,
    List<ProductImageResponse> images,
    long version,
    Instant createdAt,
    Instant updatedAt) {}
