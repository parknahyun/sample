package com.example.commerce.product.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.example.commerce.audit.ChangeAction;
import com.example.commerce.audit.ChangeLogRecorder;
import com.example.commerce.category.Category;
import com.example.commerce.category.CategoryRepository;
import com.example.commerce.common.ConflictException;
import com.example.commerce.common.NotFoundException;
import com.example.commerce.common.ValidationFailedException;
import com.example.commerce.product.api.ProductResponse;
import com.example.commerce.product.domain.Product;
import com.example.commerce.product.repository.ProductRepository;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 수정(FR-007~FR-009, FR-013, FR-016). 낙관적 락으로 동시 수정을 감지해, 먼저 저장된 변경이 있으면 나중 저장을 거부한다. 상태는 저장할 때마다
 * 재고 합계로 다시 결정한다(품절 자동 전환·복귀, 숨김 유지).
 */
@Service
public class ProductUpdateService {

  static final String CONFLICT_MESSAGE = "다른 사용자가 먼저 수정했습니다. 최신 내용을 다시 불러온 뒤 다시 시도하세요.";
  private static final Logger log = LoggerFactory.getLogger(ProductUpdateService.class);

  private final ProductRepository products;
  private final CategoryRepository categories;
  private final ProductAssembler assembler;
  private final ChangeLogRecorder recorder;
  private final ProductMapper mapper;

  ProductUpdateService(
      ProductRepository products,
      CategoryRepository categories,
      ProductAssembler assembler,
      ChangeLogRecorder recorder,
      ProductMapper mapper) {
    this.products = products;
    this.categories = categories;
    this.assembler = assembler;
    this.recorder = recorder;
    this.mapper = mapper;
  }

  /**
   * 상품을 수정한다.
   *
   * @param expectedVersion 운영자가 편집을 시작할 때 받은 버전
   */
  @Transactional
  public ProductResponse update(long productId, ProductWriteCommand command, long expectedVersion) {
    Product product =
        products.findById(productId).orElseThrow(() -> new NotFoundException("존재하지 않는 상품입니다."));
    if (product.getVersion() != expectedVersion) {
      log.warn(
          "동시 수정 충돌",
          kv("event", "version_conflict"),
          kv("productId", productId),
          kv("expectedVersion", expectedVersion),
          kv("currentVersion", product.getVersion()));
      throw new ConflictException(CONFLICT_MESSAGE);
    }

    try {
      ProductWriteValidator.validate(command);
    } catch (ValidationFailedException e) {
      log.warn(
          "상품 수정 검증 실패",
          kv("event", "product_validation_failed"),
          kv("productId", productId),
          kv("fields", e.getErrors()));
      throw e;
    }
    Category category =
        categories
            .findById(command.categoryId())
            .orElseThrow(() -> new ValidationFailedException("categoryId", "존재하지 않는 카테고리입니다."));

    Map<String, Object> before = ProductSnapshot.of(product);
    // 옵션을 통째로 다시 만든다. 같은 이름의 옵션을 다시 넣어도 고유 제약과 충돌하지 않도록 삭제를 먼저 반영한다.
    assembler.clearOptions(product);
    products.flush();
    assembler.apply(product, command, category);
    // 옵션·조합만 바뀌어도 상품 행이 갱신되어 낙관적 락 버전이 오르도록 한다.
    product.touch();
    products.saveAndFlush(product);

    Map<String, Object> changes = ProductSnapshot.diff(before, ProductSnapshot.of(product));
    recorder.record(productId, ChangeAction.UPDATED, changes);
    log.info(
        "상품 수정",
        kv("event", "product_updated"),
        kv("productId", productId),
        kv("changedFields", ProductSnapshot.changedFields(changes)));
    return mapper.toResponse(product);
  }
}
