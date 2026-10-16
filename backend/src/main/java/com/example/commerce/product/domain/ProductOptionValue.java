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

/** 옵션 값(예: 빨강, M). 옵션 그룹 안에서 고유하다. */
@Entity
@Table(name = "product_option_values")
public class ProductOptionValue {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "option_group_id", nullable = false)
  private ProductOptionGroup optionGroup;

  @Column(name = "value", nullable = false, length = 50)
  private String value;

  @Column(nullable = false)
  private int position;

  protected ProductOptionValue() {}

  ProductOptionValue(ProductOptionGroup optionGroup, String value, int position) {
    this.optionGroup = optionGroup;
    this.value = value;
    this.position = position;
  }

  public Long getId() {
    return id;
  }

  public String getValue() {
    return value;
  }

  public int getPosition() {
    return position;
  }
}
