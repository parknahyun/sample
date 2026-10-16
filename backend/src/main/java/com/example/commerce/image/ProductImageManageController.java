package com.example.commerce.image;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 상품 이미지 삭제와 대표 이미지 지정 API(운영자 전용). */
@RestController
@RequestMapping("/api/v1/products/{productId}/images/{imageId}")
public class ProductImageManageController {

  private final ProductImageManageService service;

  ProductImageManageController(ProductImageManageService service) {
    this.service = service;
  }

  @DeleteMapping
  public ResponseEntity<Void> delete(@PathVariable long productId, @PathVariable long imageId) {
    service.delete(productId, imageId);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/primary")
  public ResponseEntity<Void> setPrimary(@PathVariable long productId, @PathVariable long imageId) {
    service.setPrimary(productId, imageId);
    return ResponseEntity.noContent().build();
  }
}
