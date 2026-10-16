package com.example.commerce.product.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.example.commerce.audit.ChangeAction;
import com.example.commerce.audit.ChangeLogRecorder;
import com.example.commerce.common.NotFoundException;
import com.example.commerce.product.domain.Product;
import com.example.commerce.product.repository.ProductRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 삭제(FR-012). 모든 상품을 논리 삭제한다. 삭제 표시(deleted_at)만 기록하고 상품·옵션·이미지 데이터는 보존하므로 과거 주문 정보를 계속 조회할 수
 * 있다(SC-006). 삭제된 상품은 운영자 목록과 고객 화면에서 제외된다.
 */
@Service
public class ProductDeleteService {

  private static final Logger log = LoggerFactory.getLogger(ProductDeleteService.class);

  private final ProductRepository products;
  private final ChangeLogRecorder recorder;

  ProductDeleteService(ProductRepository products, ChangeLogRecorder recorder) {
    this.products = products;
    this.recorder = recorder;
  }

  @Transactional
  public void delete(long productId) {
    Product product =
        products.findById(productId).orElseThrow(() -> new NotFoundException("존재하지 않는 상품입니다."));
    product.markDeleted();
    products.saveAndFlush(product);

    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("code", product.getCode());
    summary.put("name", product.getName());
    recorder.record(productId, ChangeAction.DELETED, summary);
    log.info(
        "상품 삭제",
        kv("event", "product_deleted"),
        kv("productId", productId),
        kv("code", product.getCode()));
  }
}
