package com.example.commerce.image;

import com.example.commerce.product.api.ProductImageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 상품 이미지 업로드 API(운영자 전용). */
@RestController
@RequestMapping("/api/v1/products/{productId}/images")
public class ProductImageController {

  private final ProductImageUploadService uploadService;

  ProductImageController(ProductImageUploadService uploadService) {
    this.uploadService = uploadService;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ProductImageResponse> upload(
      @PathVariable long productId, @RequestPart("file") MultipartFile file) {
    return ResponseEntity.status(HttpStatus.CREATED).body(uploadService.upload(productId, file));
  }
}
