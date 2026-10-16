package com.example.commerce.product.service;

import com.example.commerce.product.domain.Product;
import com.example.commerce.product.domain.ProductOptionGroup;
import com.example.commerce.product.domain.ProductOptionValue;
import com.example.commerce.product.domain.ProductVariant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 변경 이력에 남길 상품 스냅샷과 두 스냅샷의 차이(변경 필드와 이전/이후 값). */
final class ProductSnapshot {

  private ProductSnapshot() {}

  static Map<String, Object> of(Product product) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("code", product.getCode());
    snapshot.put("name", product.getName());
    snapshot.put("description", product.getDescription());
    snapshot.put("price", product.getPrice());
    snapshot.put("categoryId", product.getCategory().getId());
    snapshot.put("status", product.getStatus().name());
    snapshot.put("stockQuantity", product.getStockQuantity());
    snapshot.put(
        "optionGroups",
        product.getOptionGroups().stream()
            .map(
                (ProductOptionGroup group) -> {
                  Map<String, Object> entry = new LinkedHashMap<>();
                  entry.put("name", group.getName());
                  entry.put(
                      "values",
                      group.getValues().stream().map(ProductOptionValue::getValue).toList());
                  return entry;
                })
            .toList());
    Map<String, Object> variants = new LinkedHashMap<>();
    for (ProductVariant variant : product.getVariants()) {
      variants.put(variant.getCombinationKey(), variant.getStockQuantity());
    }
    snapshot.put("variants", variants);
    return snapshot;
  }

  /** 값이 달라진 필드만 {@code {필드: {before, after}}} 형태로 돌려준다. */
  static Map<String, Object> diff(Map<String, Object> before, Map<String, Object> after) {
    Map<String, Object> changes = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : after.entrySet()) {
      Object previous = before.get(entry.getKey());
      if (!Objects.equals(previous, entry.getValue())) {
        Map<String, Object> change = new LinkedHashMap<>();
        change.put("before", previous);
        change.put("after", entry.getValue());
        changes.put(entry.getKey(), change);
      }
    }
    return changes;
  }

  static List<String> changedFields(Map<String, Object> changes) {
    return List.copyOf(changes.keySet());
  }
}
