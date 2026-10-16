package com.example.commerce.storefront;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 고객 노출용 읽기 전용 상품 조회 API(인증 불필요). */
@RestController
@RequestMapping("/api/v1/storefront/products")
public class StorefrontController {

  private final StorefrontQueryService service;

  StorefrontController(StorefrontQueryService service) {
    this.service = service;
  }

  @GetMapping
  public StorefrontProductPageResponse list(
      @RequestParam(required = false) Long categoryId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.list(categoryId, page, size);
  }

  @GetMapping("/{productId}")
  public StorefrontProductResponse get(@PathVariable long productId) {
    return service.get(productId);
  }
}
