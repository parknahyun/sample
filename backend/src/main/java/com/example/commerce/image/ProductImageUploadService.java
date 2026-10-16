package com.example.commerce.image;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.example.commerce.audit.ChangeAction;
import com.example.commerce.audit.ChangeLogRecorder;
import com.example.commerce.common.NotFoundException;
import com.example.commerce.common.ValidationFailedException;
import com.example.commerce.product.api.ProductImageResponse;
import com.example.commerce.product.domain.Product;
import com.example.commerce.product.domain.ProductImage;
import com.example.commerce.product.repository.ProductImageRepository;
import com.example.commerce.product.repository.ProductRepository;
import com.example.commerce.product.service.ProductMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 업로드(FR-002). 상품당 10장 이하, JPG·PNG, 장당 5MB 이하만 허용한다. 첫 이미지는 자동으로 대표가 된다.
 *
 * <p>이미지는 상품 행을 바꾸지 않으므로 상품 버전(동시 수정 감지)에 영향을 주지 않는다.
 */
@Service
public class ProductImageUploadService {

  public static final int MAX_IMAGES_PER_PRODUCT = 10;
  private static final Logger log = LoggerFactory.getLogger(ProductImageUploadService.class);

  private final ProductRepository products;
  private final ProductImageRepository images;
  private final ImageStorage storage;
  private final ImageValidator validator;
  private final ChangeLogRecorder recorder;
  private final ProductMapper mapper;

  ProductImageUploadService(
      ProductRepository products,
      ProductImageRepository images,
      ImageStorage storage,
      ImageValidator validator,
      ChangeLogRecorder recorder,
      ProductMapper mapper) {
    this.products = products;
    this.images = images;
    this.storage = storage;
    this.validator = validator;
    this.recorder = recorder;
    this.mapper = mapper;
  }

  @Transactional
  public ProductImageResponse upload(long productId, MultipartFile file) {
    Product product =
        products.findById(productId).orElseThrow(() -> new NotFoundException("존재하지 않는 상품입니다."));
    byte[] content = read(file);

    String contentType;
    try {
      contentType = validator.detectContentType(content);
    } catch (ValidationFailedException e) {
      log.warn(
          "이미지 업로드 거부",
          kv("event", "image_rejected"),
          kv("productId", productId),
          kv("reason", e.getMessage()));
      throw e;
    }

    int count = images.countByProductId(productId);
    if (count >= MAX_IMAGES_PER_PRODUCT) {
      log.warn(
          "이미지 업로드 거부",
          kv("event", "image_rejected"),
          kv("productId", productId),
          kv("reason", "max_images"));
      throw new ValidationFailedException(
          "file", "이미지는 상품당 최대 " + MAX_IMAGES_PER_PRODUCT + "장까지 올릴 수 있습니다.");
    }

    String key = UUID.randomUUID() + "." + ImageValidator.extensionOf(contentType);
    storage.save(key, content);
    // 이후 DB 반영이 취소되면 방금 저장한 파일도 지운다.
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) {
              storage.delete(key);
            }
          }
        });

    ProductImage image =
        images.saveAndFlush(
            new ProductImage(
                product,
                key,
                contentType,
                content.length,
                count == 0,
                images.nextPosition(productId)));

    Map<String, Object> uploaded = new LinkedHashMap<>();
    uploaded.put("id", image.getId());
    uploaded.put("contentType", contentType);
    uploaded.put("sizeBytes", content.length);
    uploaded.put("primary", image.isPrimary());
    recorder.record(productId, ChangeAction.UPDATED, Map.of("image", Map.of("uploaded", uploaded)));
    log.info(
        "이미지 업로드",
        kv("event", "image_uploaded"),
        kv("productId", productId),
        kv("imageId", image.getId()));
    return mapper.toImageResponse(image);
  }

  private static byte[] read(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException e) {
      throw new ValidationFailedException("file", "파일을 읽을 수 없습니다. 다시 올려 주세요.");
    }
  }
}
