package com.example.commerce.image;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.example.commerce.audit.ChangeAction;
import com.example.commerce.audit.ChangeLogRecorder;
import com.example.commerce.common.NotFoundException;
import com.example.commerce.product.domain.ProductImage;
import com.example.commerce.product.repository.ProductImageRepository;
import com.example.commerce.product.repository.ProductRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 이미지 삭제와 대표 이미지 지정(FR-002). 대표 이미지는 이미지가 있으면 정확히 하나이며, 대표를 삭제하면 표시 순서가 가장 앞선 남은 이미지가 대표가 된다. 마지막 한
 * 장도 삭제할 수 있다(이미지는 선택 항목).
 *
 * <p>이미지는 상품 행을 바꾸지 않으므로 상품 버전(동시 수정 감지)에 영향을 주지 않는다.
 */
@Service
public class ProductImageManageService {

  private static final Logger log = LoggerFactory.getLogger(ProductImageManageService.class);

  private final ProductRepository products;
  private final ProductImageRepository images;
  private final ImageStorage storage;
  private final ChangeLogRecorder recorder;

  ProductImageManageService(
      ProductRepository products,
      ProductImageRepository images,
      ImageStorage storage,
      ChangeLogRecorder recorder) {
    this.products = products;
    this.images = images;
    this.storage = storage;
    this.recorder = recorder;
  }

  @Transactional
  public void delete(long productId, long imageId) {
    ProductImage image = findImage(productId, imageId);
    boolean wasPrimary = image.isPrimary();
    String storageKey = image.getStorageKey();
    images.delete(image);
    images.flush();

    Map<String, Object> change = new LinkedHashMap<>();
    change.put("deleted", imageId);
    if (wasPrimary) {
      List<ProductImage> remaining = images.findByProductIdOrderByPositionAsc(productId);
      if (!remaining.isEmpty()) {
        remaining.get(0).setPrimary(true);
        change.put("primary", remaining.get(0).getId());
      }
    }
    recorder.record(productId, ChangeAction.UPDATED, Map.of("image", change));
    // 파일은 DB 반영이 확정된 뒤에만 지운다.
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            storage.delete(storageKey);
          }
        });
    log.info(
        "이미지 삭제", kv("event", "image_deleted"), kv("productId", productId), kv("imageId", imageId));
  }

  @Transactional
  public void setPrimary(long productId, long imageId) {
    ProductImage target = findImage(productId, imageId);
    for (ProductImage image : images.findByProductIdOrderByPositionAsc(productId)) {
      image.setPrimary(image.getId().equals(target.getId()));
    }
    recorder.record(productId, ChangeAction.UPDATED, Map.of("image", Map.of("primary", imageId)));
    log.info(
        "대표 이미지 변경",
        kv("event", "image_primary_changed"),
        kv("productId", productId),
        kv("imageId", imageId));
  }

  private ProductImage findImage(long productId, long imageId) {
    products.findById(productId).orElseThrow(() -> new NotFoundException("존재하지 않는 상품입니다."));
    return images
        .findByIdAndProductId(imageId, productId)
        .orElseThrow(() -> new NotFoundException("존재하지 않는 이미지입니다."));
  }
}
