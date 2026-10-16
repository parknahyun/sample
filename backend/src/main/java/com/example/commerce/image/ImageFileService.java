package com.example.commerce.image;

import com.example.commerce.common.NotFoundException;
import com.example.commerce.product.repository.ProductImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 이미지 파일 조회. 삭제된 상품의 이미지는 제공하지 않는다. */
@Service
public class ImageFileService {

  /** 응답에 쓸 이미지 파일. */
  public record ImageFile(String contentType, byte[] content) {}

  private final ProductImageRepository images;
  private final ImageStorage storage;

  ImageFileService(ProductImageRepository images, ImageStorage storage) {
    this.images = images;
    this.storage = storage;
  }

  @Transactional(readOnly = true)
  public ImageFile find(String storageKey) {
    if (!LocalImageStorage.KEY_FORMAT.matcher(storageKey).matches()) {
      throw new NotFoundException("존재하지 않는 이미지입니다.");
    }
    return images
        .findServable(storageKey)
        .flatMap(
            image ->
                storage.load(storageKey).map(bytes -> new ImageFile(image.getContentType(), bytes)))
        .orElseThrow(() -> new NotFoundException("존재하지 않는 이미지입니다."));
  }
}
