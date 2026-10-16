package com.example.commerce.image;

import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이미지 파일 제공. 고객 화면에서도 표시해야 하므로 인증 없이 읽을 수 있다. 키는 추측하기 어려운 서버 생성 UUID이고, 목록에 노출되지 않는 숨김 상품의 이미지도 이
 * 키로만 접근할 수 있다.
 */
@RestController
@RequestMapping("/api/v1/images")
public class ImageFileController {

  private final ImageFileService service;

  ImageFileController(ImageFileService service) {
    this.service = service;
  }

  @GetMapping("/{storageKey}")
  public ResponseEntity<byte[]> get(@PathVariable String storageKey) {
    ImageFileService.ImageFile file = service.find(storageKey);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.contentType()))
        .header("X-Content-Type-Options", "nosniff")
        .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
        .body(file.content());
  }
}
