package com.example.commerce.audit;

import java.time.Instant;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 변경 이력 기록기(FR-013). 상품 변경과 같은 트랜잭션에서만 저장할 수 있어(MANDATORY), 상품 저장이 실패하면 이력도 함께 취소된다. */
@Component
public class ChangeLogRecorder {

  private static final String SYSTEM_USER = "system";

  private final ProductChangeLogRepository repository;

  public ChangeLogRecorder(ProductChangeLogRepository repository) {
    this.repository = repository;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void record(Long productId, ChangeAction action, Map<String, Object> summary) {
    repository.save(new ProductChangeLog(productId, action, currentUser(), Instant.now(), summary));
  }

  private static String currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null) {
      return SYSTEM_USER;
    }
    String name = authentication.getName();
    return name.length() > 100 ? name.substring(0, 100) : name;
  }
}
