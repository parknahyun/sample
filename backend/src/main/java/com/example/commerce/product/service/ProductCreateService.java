package com.example.commerce.product.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.example.commerce.audit.ChangeAction;
import com.example.commerce.audit.ChangeLogRecorder;
import com.example.commerce.category.Category;
import com.example.commerce.category.CategoryRepository;
import com.example.commerce.common.ConflictException;
import com.example.commerce.common.ValidationFailedException;
import com.example.commerce.product.api.ProductResponse;
import com.example.commerce.product.domain.Product;
import com.example.commerce.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 상품 등록(FR-001, FR-003~FR-005, FR-013, FR-015). */
@Service
public class ProductCreateService {

  private static final Logger log = LoggerFactory.getLogger(ProductCreateService.class);

  private final ProductRepository products;
  private final CategoryRepository categories;
  private final ProductCodeGenerator codes;
  private final ProductAssembler assembler;
  private final ChangeLogRecorder recorder;
  private final ProductMapper mapper;

  ProductCreateService(
      ProductRepository products,
      CategoryRepository categories,
      ProductCodeGenerator codes,
      ProductAssembler assembler,
      ChangeLogRecorder recorder,
      ProductMapper mapper) {
    this.products = products;
    this.categories = categories;
    this.codes = codes;
    this.assembler = assembler;
    this.recorder = recorder;
    this.mapper = mapper;
  }

  /**
   * 상품을 등록한다.
   *
   * @param requestedCode 운영자가 입력한 상품 코드(비어 있으면 자동 생성)
   */
  @Transactional
  public ProductResponse create(ProductWriteCommand command, String requestedCode) {
    try {
      ProductWriteValidator.validate(command);
    } catch (ValidationFailedException e) {
      log.warn(
          "상품 등록 검증 실패", kv("event", "product_validation_failed"), kv("fields", e.getErrors()));
      throw e;
    }
    Category category =
        categories
            .findById(command.categoryId())
            .orElseThrow(() -> new ValidationFailedException("categoryId", "존재하지 않는 카테고리입니다."));
    String code = codes.resolve(requestedCode);

    Product product = new Product(code, command.name().trim(), command.price(), category);
    assembler.apply(product, command, category);
    try {
      products.saveAndFlush(product);
    } catch (DataIntegrityViolationException e) {
      // 같은 코드를 동시에 등록한 경우 고유 제약이 마지막 방어선이다.
      if (e.getMessage() != null && e.getMessage().contains("products_code_key")) {
        throw new ConflictException("이미 사용 중인 상품 코드입니다: " + code + ". 다른 코드를 입력하세요.");
      }
      throw e;
    }

    recorder.record(product.getId(), ChangeAction.CREATED, ProductSnapshot.of(product));
    log.info(
        "상품 등록",
        kv("event", "product_created"),
        kv("productId", product.getId()),
        kv("code", code));
    return mapper.toResponse(product);
  }
}
