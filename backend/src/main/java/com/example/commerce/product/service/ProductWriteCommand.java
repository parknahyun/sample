package com.example.commerce.product.service;

import com.example.commerce.product.api.OptionGroupDto;
import com.example.commerce.product.api.VariantDto;
import com.example.commerce.product.domain.ProductStatus;
import java.util.List;

/** 등록·수정 요청이 공통으로 담는 상품 정보. */
public record ProductWriteCommand(
    String name,
    String description,
    Long price,
    Long categoryId,
    ProductStatus status,
    Integer stockQuantity,
    List<OptionGroupDto> optionGroups,
    List<VariantDto> variants) {

  public List<OptionGroupDto> optionGroupsOrEmpty() {
    return optionGroups == null ? List.of() : optionGroups;
  }

  public List<VariantDto> variantsOrEmpty() {
    return variants == null ? List.of() : variants;
  }

  /** 운영자가 지정한 상태(비어 있으면 판매중). */
  public ProductStatus desiredStatus() {
    return status == null ? ProductStatus.ON_SALE : status;
  }
}
