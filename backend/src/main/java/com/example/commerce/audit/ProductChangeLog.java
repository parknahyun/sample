package com.example.commerce.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 상품의 생성·수정·삭제 이력. 추가만 가능하며 수정·삭제하지 않는다. */
@Entity
@Table(name = "product_change_logs")
public class ProductChangeLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "product_id", nullable = false, updatable = false)
  private Long productId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 10)
  private ChangeAction action;

  @Column(name = "changed_by", nullable = false, updatable = false, length = 100)
  private String changedBy;

  @Column(name = "changed_at", nullable = false, updatable = false)
  private Instant changedAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(updatable = false)
  private Map<String, Object> summary;

  protected ProductChangeLog() {}

  public ProductChangeLog(
      Long productId,
      ChangeAction action,
      String changedBy,
      Instant changedAt,
      Map<String, Object> summary) {
    this.productId = productId;
    this.action = action;
    this.changedBy = changedBy;
    this.changedAt = changedAt;
    this.summary = summary;
  }

  public Long getId() {
    return id;
  }

  public Long getProductId() {
    return productId;
  }

  public ChangeAction getAction() {
    return action;
  }

  public String getChangedBy() {
    return changedBy;
  }

  public Instant getChangedAt() {
    return changedAt;
  }

  public Map<String, Object> getSummary() {
    return summary;
  }
}
