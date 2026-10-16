package com.example.commerce.product.api;

import com.example.commerce.product.service.ProductCreateService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 상품 등록 API(운영자 전용). */
@RestController
@RequestMapping("/api/v1/products")
public class ProductCreateController {

  private final ProductCreateService service;

  ProductCreateController(ProductCreateService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
    ProductResponse created = service.create(request.toCommand(), request.code());
    return ResponseEntity.created(URI.create("/api/v1/products/" + created.id())).body(created);
  }
}
