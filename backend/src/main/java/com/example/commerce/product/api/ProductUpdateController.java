package com.example.commerce.product.api;

import com.example.commerce.product.service.ProductUpdateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 상품 수정 API(운영자 전용). */
@RestController
@RequestMapping("/api/v1/products")
public class ProductUpdateController {

  private final ProductUpdateService service;

  ProductUpdateController(ProductUpdateService service) {
    this.service = service;
  }

  @PutMapping("/{productId}")
  public ProductResponse update(
      @PathVariable long productId, @Valid @RequestBody ProductUpdateRequest request) {
    return service.update(productId, request.toCommand(), request.version());
  }
}
