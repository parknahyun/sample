package com.example.commerce.product.domain;

import com.example.commerce.category.Category;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.SQLRestriction;

/**
 * 판매 대상 상품. 삭제는 논리 삭제(deleted_at 기록)이며, 삭제된 행은 기본 조회에서 제외된다.
 *
 * <p>상품 코드는 한 번 정해지면 바꿀 수 없다(updatable = false).
 */
@Entity
@Table(name = "products")
@SQLRestriction("deleted_at IS NULL")
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false, length = 30)
  private String code;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(columnDefinition = "text")
  private String description;

  @Column(nullable = false)
  private long price;

  /** 옵션이 없는 상품의 재고. 옵션 상품은 null. */
  @Column(name = "stock_quantity")
  private Integer stockQuantity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ProductStatus status;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;

  @Version private long version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position ASC")
  private List<ProductOptionGroup> optionGroups = new ArrayList<>();

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id ASC")
  private List<ProductVariant> variants = new ArrayList<>();

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position ASC")
  private List<ProductImage> images = new ArrayList<>();

  protected Product() {}

  public Product(String code, String name, long price, Category category) {
    this.code = code;
    this.name = name;
    this.price = price;
    this.category = category;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  /** 수정 시각을 갱신한다. 하위 데이터(옵션, 이미지)만 바뀌어도 낙관적 락 버전이 올라가도록 상품 행을 변경한다. */
  public void touch() {
    this.updatedAt = Instant.now();
  }

  public void markDeleted() {
    this.deletedAt = Instant.now();
    touch();
  }

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public long getPrice() {
    return price;
  }

  public void setPrice(long price) {
    this.price = price;
  }

  public Integer getStockQuantity() {
    return stockQuantity;
  }

  public void setStockQuantity(Integer stockQuantity) {
    this.stockQuantity = stockQuantity;
  }

  public ProductStatus getStatus() {
    return status;
  }

  public void setStatus(ProductStatus status) {
    this.status = status;
  }

  public Category getCategory() {
    return category;
  }

  public void setCategory(Category category) {
    this.category = category;
  }

  public long getVersion() {
    return version;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public List<ProductOptionGroup> getOptionGroups() {
    return optionGroups;
  }

  public List<ProductVariant> getVariants() {
    return variants;
  }

  public List<ProductImage> getImages() {
    return images;
  }
}
