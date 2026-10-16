package com.example.commerce.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** 옵션 값의 조합과 그 조합의 재고. 가격은 상품 단위로만 가진다. */
@Entity
@Table(name = "product_variants")
public class ProductVariant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "combination_key", nullable = false, length = 300)
  private String combinationKey;

  @Column(name = "stock_quantity", nullable = false)
  private int stockQuantity;

  protected ProductVariant() {}

  public ProductVariant(Product product, String combinationKey, int stockQuantity) {
    this.product = product;
    this.combinationKey = combinationKey;
    this.stockQuantity = stockQuantity;
  }

  public Long getId() {
    return id;
  }

  public String getCombinationKey() {
    return combinationKey;
  }

  public int getStockQuantity() {
    return stockQuantity;
  }
}
