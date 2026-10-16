package com.example.commerce.product.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/** 옵션 값 조합(예: 색상=빨강, 사이즈=M)과 그 조합의 재고. */
public record VariantDto(
    @NotNull(message = "옵션 값 조합은 필수입니다") Map<String, String> optionValues,
    @NotNull(message = "옵션 조합의 재고는 필수입니다") @Min(value = 0, message = "재고는 0 이상의 정수여야 합니다")
        Integer stockQuantity) {}
