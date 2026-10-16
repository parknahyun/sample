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

/** 상품 이미지 메타데이터. 파일 자체는 ImageStorage에 서버가 만든 UUID 파일명으로 저장한다. */
@Entity
@Table(name = "product_images")
public class ProductImage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "storage_key", nullable = false, length = 100, unique = true)
  private String storageKey;

  @Column(name = "content_type", nullable = false, length = 20)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private int sizeBytes;

  @Column(name = "is_primary", nullable = false)
  private boolean primary;

  @Column(nullable = false)
  private int position;

  protected ProductImage() {}

  public ProductImage(
      Product product,
      String storageKey,
      String contentType,
      int sizeBytes,
      boolean primary,
      int position) {
    this.product = product;
    this.storageKey = storageKey;
    this.contentType = contentType;
    this.sizeBytes = sizeBytes;
    this.primary = primary;
    this.position = position;
  }

  public Long getId() {
    return id;
  }

  public Product getProduct() {
    return product;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public String getContentType() {
    return contentType;
  }

  public int getSizeBytes() {
    return sizeBytes;
  }

  public boolean isPrimary() {
    return primary;
  }

  public void setPrimary(boolean primary) {
    this.primary = primary;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }
}
