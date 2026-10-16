package com.example.commerce.product.service;

import com.example.commerce.category.Category;
import com.example.commerce.product.api.OptionGroupDto;
import com.example.commerce.product.api.VariantDto;
import com.example.commerce.product.domain.Product;
import com.example.commerce.product.domain.ProductOptionGroup;
import com.example.commerce.product.domain.ProductStatusPolicy;
import com.example.commerce.product.domain.ProductVariant;
import com.example.commerce.product.domain.VariantCombinationKey;
import java.util.List;
import org.springframework.stereotype.Component;

/** 등록·수정 요청의 내용을 상품 엔터티에 반영한다. 옵션 정의와 옵션 조합은 통째로 다시 만든다. */
@Component
class ProductAssembler {

  /** 기존 옵션 정의와 옵션 조합을 비운다. 새 옵션을 넣기 전에 호출하고 flush해야 고유 제약과 충돌하지 않는다. */
  void clearOptions(Product product) {
    product.getVariants().clear();
    product.getOptionGroups().clear();
  }

  /** 옵션이 비어 있는 상품에 요청 내용을 반영한다(옵션 정의·조합 포함, 상태는 재고 합계로 결정). */
  void apply(Product product, ProductWriteCommand command, Category category) {
    product.setName(command.name().trim());
    product.setDescription(blankToNull(command.description()));
    product.setPrice(command.price());
    product.setCategory(category);

    List<OptionGroupDto> groups = command.optionGroupsOrEmpty();
    int position = 0;
    for (OptionGroupDto group : groups) {
      ProductOptionGroup optionGroup = new ProductOptionGroup(product, group.name(), position++);
      group.values().forEach(optionGroup::addValue);
      product.getOptionGroups().add(optionGroup);
    }

    List<String> groupOrder = groups.stream().map(OptionGroupDto::name).toList();
    for (VariantDto variant : command.variantsOrEmpty()) {
      String key = VariantCombinationKey.key(groupOrder, variant.optionValues());
      product.getVariants().add(new ProductVariant(product, key, variant.stockQuantity()));
    }

    if (groups.isEmpty()) {
      product.setStockQuantity(command.stockQuantity() == null ? 0 : command.stockQuantity());
    } else {
      product.setStockQuantity(null);
    }
    long total =
        ProductStatusPolicy.totalStock(
            product.getStockQuantity(),
            product.getVariants().stream().map(ProductVariant::getStockQuantity).toList());
    product.setStatus(ProductStatusPolicy.resolve(command.desiredStatus(), total));
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
