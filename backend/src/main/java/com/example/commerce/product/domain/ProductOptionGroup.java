package com.example.commerce.product.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/** 옵션 그룹(예: 색상, 사이즈). 상품 안에서 이름이 고유하다. */
@Entity
@Table(name = "product_option_groups")
public class ProductOptionGroup {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(nullable = false)
  private int position;

  @OneToMany(mappedBy = "optionGroup", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position ASC")
  private List<ProductOptionValue> values = new ArrayList<>();

  protected ProductOptionGroup() {}

  public ProductOptionGroup(Product product, String name, int position) {
    this.product = product;
    this.name = name;
    this.position = position;
  }

  public ProductOptionValue addValue(String value) {
    ProductOptionValue optionValue = new ProductOptionValue(this, value, values.size());
    values.add(optionValue);
    return optionValue;
  }

  public Long getId() {
    return id;
  }

  public Product getProduct() {
    return product;
  }

  public String getName() {
    return name;
  }

  public int getPosition() {
    return position;
  }

  public List<ProductOptionValue> getValues() {
    return values;
  }
}
