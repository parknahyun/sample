package com.example.commerce.storefront;

import com.example.commerce.product.api.OptionGroupDto;
import com.example.commerce.product.api.ProductImageResponse;
import java.util.List;
import java.util.Map;

/** 고객 노출용 상품. 재고 수량·상품 코드·버전 같은 내부 정보는 포함하지 않고 품절 여부만 담는다(FR-018). */
public record StorefrontProductResponse(
    Long id,
    String name,
    String description,
    long price,
    Long categoryId,
    boolean soldOut,
    List<OptionGroupDto> optionGroups,
    List<StorefrontVariantResponse> variants,
    List<ProductImageResponse> images) {

  /** 옵션 조합과 품절 여부. */
  public record StorefrontVariantResponse(Map<String, String> optionValues, boolean soldOut) {}
}
