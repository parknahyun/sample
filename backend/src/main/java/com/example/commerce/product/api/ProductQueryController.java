package com.example.commerce.product.api;

import com.example.commerce.product.domain.ProductStatus;
import com.example.commerce.product.service.ProductQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 상품 목록·검색·상세 조회 API(운영자 전용). */
@RestController
@RequestMapping("/api/v1/products")
public class ProductQueryController {

  private final ProductQueryService service;

  ProductQueryController(ProductQueryService service) {
    this.service = service;
  }

  @GetMapping
  public ProductPageResponse list(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) ProductStatus status,
      @RequestParam(required = false) String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "" + ProductQueryService.DEFAULT_SIZE) int size) {
    return service.list(q, categoryId, status, sort, page, size);
  }

  @GetMapping("/{productId}")
  public ProductResponse get(@PathVariable long productId) {
    return service.get(productId);
  }
}
