package com.example.commerce.product.domain;

/** 상품 판매 상태. 운영자는 ON_SALE과 HIDDEN만 지정하고, SOLD_OUT은 재고로 결정된다. */
public enum ProductStatus {
  ON_SALE,
  SOLD_OUT,
  HIDDEN
}
