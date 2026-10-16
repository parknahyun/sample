package com.example.commerce.support;

import com.example.commerce.common.ConflictException;
import com.example.commerce.common.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** 보안 설정과 오류 응답 형식을 검증하기 위한 테스트 전용 컨트롤러. */
@RestController
public class ProbeController {

  public record ProbeRequest(
      @NotBlank(message = "이름은 필수입니다") String name,
      @Min(value = 1, message = "수량은 1 이상이어야 합니다") Integer count) {}

  @GetMapping("/api/v1/probe")
  public String managerOnly() {
    return "ok";
  }

  @GetMapping("/api/v1/storefront/probe")
  public String storefrontGet() {
    return "ok";
  }

  @PostMapping("/api/v1/storefront/probe")
  public String storefrontPost() {
    return "ok";
  }

  @GetMapping("/api/v1/images/probe")
  public String imageGet() {
    return "ok";
  }

  @PostMapping("/api/v1/probe/validate")
  public String validate(@Valid @RequestBody ProbeRequest request) {
    return "ok";
  }

  @GetMapping("/api/v1/probe/conflict")
  public String conflict() {
    throw new ConflictException("이미 사용 중인 코드입니다. 다른 코드를 입력하세요.");
  }

  @GetMapping("/api/v1/probe/not-found")
  public String notFound() {
    throw new NotFoundException("존재하지 않는 상품입니다.");
  }

  @GetMapping("/api/v1/probe/optimistic")
  public String optimistic() {
    throw new ObjectOptimisticLockingFailureException("Product", 1L);
  }

  @GetMapping("/api/v1/probe/too-large")
  public String tooLarge() {
    throw new MaxUploadSizeExceededException(5L * 1024 * 1024);
  }

  @GetMapping("/api/v1/probe/boom")
  public String boom() {
    throw new IllegalStateException("내부 구현 세부 정보: 노출되면 안 된다");
  }
}
