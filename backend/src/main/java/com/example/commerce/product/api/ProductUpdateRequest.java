package com.example.commerce.product.api;

import com.example.commerce.product.domain.ProductStatus;
import com.example.commerce.product.service.ProductWriteCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 상품 수정 요청. 편집을 시작할 때 받은 version을 함께 보내야 하며, 그 사이 다른 운영자가 먼저 저장했다면 409로 거부한다. 상품 코드는 수정할 수 없으므로 요청에
 * 포함되어도 무시한다.
 */
public record ProductUpdateRequest(
    @NotBlank(message = "상품명은 필수입니다") @Size(max = 200, message = "상품명은 200자 이하여야 합니다") String name,
    String description,
    @NotNull(message = "가격은 필수입니다") @Min(value = 1, message = "가격은 0보다 큰 정수여야 합니다") Long price,
    @NotNull(message = "카테고리는 필수입니다") Long categoryId,
    ProductStatus status,
    @Min(value = 0, message = "재고는 0 이상의 정수여야 합니다") Integer stockQuantity,
    @Valid List<OptionGroupDto> optionGroups,
    @Valid List<VariantDto> variants,
    @NotNull(message = "version은 필수입니다") Long version) {

  public ProductWriteCommand toCommand() {
    return new ProductWriteCommand(
        name, description, price, categoryId, status, stockQuantity, optionGroups, variants);
  }
}
