package com.example.commerce.product.api;

import com.example.commerce.product.domain.ProductStatus;
import com.example.commerce.product.service.ProductWriteCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 상품 등록 요청. 필수 항목은 상품명·가격·카테고리다(FR-003). */
public record ProductCreateRequest(
    @Pattern(regexp = "^(|[A-Z0-9-]{3,30})$", message = "상품 코드는 영문 대문자, 숫자, 하이픈 3~30자여야 합니다")
        String code,
    @NotBlank(message = "상품명은 필수입니다") @Size(max = 200, message = "상품명은 200자 이하여야 합니다") String name,
    String description,
    @NotNull(message = "가격은 필수입니다") @Min(value = 1, message = "가격은 0보다 큰 정수여야 합니다") Long price,
    @NotNull(message = "카테고리는 필수입니다") Long categoryId,
    ProductStatus status,
    @Min(value = 0, message = "재고는 0 이상의 정수여야 합니다") Integer stockQuantity,
    @Valid List<OptionGroupDto> optionGroups,
    @Valid List<VariantDto> variants) {

  public ProductWriteCommand toCommand() {
    return new ProductWriteCommand(
        name, description, price, categoryId, status, stockQuantity, optionGroups, variants);
  }
}
