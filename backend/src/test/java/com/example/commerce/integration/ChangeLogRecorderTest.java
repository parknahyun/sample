package com.example.commerce.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.commerce.audit.ChangeAction;
import com.example.commerce.audit.ChangeLogRecorder;
import com.example.commerce.audit.ProductChangeLog;
import com.example.commerce.audit.ProductChangeLogRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/** FR-013: 변경 이력은 상품 변경과 같은 트랜잭션에서 저장되고, 변경자·시각·작업 종류·요약이 기록된다. */
class ChangeLogRecorderTest extends IntegrationTestBase {

  @Autowired ChangeLogRecorder recorder;
  @Autowired ProductChangeLogRepository repository;
  @Autowired TransactionTemplate transactions;

  @Test
  void 변경자_시각_작업종류_요약이_기록된다() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "운영자A", null, AuthorityUtils.createAuthorityList("ROLE_PRODUCT_MANAGER")));
    try {
      transactions.executeWithoutResult(
          s ->
              recorder.record(
                  7L,
                  ChangeAction.UPDATED,
                  Map.of("price", Map.of("before", 1000, "after", 2000))));
    } finally {
      SecurityContextHolder.clearContext();
    }

    List<ProductChangeLog> logs = repository.findByProductIdOrderByIdAsc(7L);
    assertThat(logs).hasSize(1);
    ProductChangeLog log = logs.get(0);
    assertThat(log.getAction()).isEqualTo(ChangeAction.UPDATED);
    assertThat(log.getChangedBy()).isEqualTo("운영자A");
    assertThat(log.getChangedAt()).isNotNull();
    assertThat(log.getSummary()).containsKey("price");
  }

  @Test
  void 트랜잭션_밖에서는_기록할_수_없다() {
    assertThatThrownBy(() -> recorder.record(1L, ChangeAction.CREATED, Map.of()))
        .isInstanceOf(IllegalTransactionStateException.class);
  }

  @Test
  void 트랜잭션이_롤백되면_이력도_함께_취소된다() {
    assertThatThrownBy(
            () ->
                transactions.executeWithoutResult(
                    s -> {
                      recorder.record(9L, ChangeAction.CREATED, Map.of("name", "상품"));
                      throw new IllegalStateException("상품 저장 실패");
                    }))
        .isInstanceOf(IllegalStateException.class);

    assertThat(repository.findByProductIdOrderByIdAsc(9L)).isEmpty();
  }
}
