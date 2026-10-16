package com.example.commerce.product.api;

import com.example.commerce.product.service.ProductDeleteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 상품 삭제 API(운영자 전용). 삭제 전 확인 절차는 클라이언트 화면이 맡고, 서버는 논리 삭제만 한다. */
@RestController
@RequestMapping("/api/v1/products")
public class ProductDeleteController {

  private final ProductDeleteService service;

  ProductDeleteController(ProductDeleteService service) {
    this.service = service;
  }

  @DeleteMapping("/{productId}")
  public ResponseEntity<Void> delete(@PathVariable long productId) {
    service.delete(productId);
    return ResponseEntity.noContent().build();
  }
}
