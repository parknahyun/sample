package com.example.commerce.product.service;

import com.example.commerce.common.ConflictException;
import com.example.commerce.product.repository.ProductRepository;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 상품 코드 생성과 검증(FR-005). 직접 입력하지 않으면 DB 시퀀스로 PRD-000001 형식을 만들고, 직접 입력하면 형식과 중복을 검사한다. 중복 검사는 삭제된
 * 상품까지 포함한다(과거 주문과 혼동되지 않도록).
 */
@Component
public class ProductCodeGenerator {

  private static final Pattern FORMAT = Pattern.compile("^[A-Z0-9-]{3,30}$");
  private static final int MAX_ATTEMPTS = 10;

  private final ProductRepository products;

  public ProductCodeGenerator(ProductRepository products) {
    this.products = products;
  }

  public static String format(long sequence) {
    return String.format("PRD-%06d", sequence);
  }

  public static boolean isValidFormat(String code) {
    return code != null && FORMAT.matcher(code).matches();
  }

  /**
   * 사용할 코드를 정한다.
   *
   * @param requested 운영자가 입력한 코드(비어 있으면 자동 생성)
   * @throws ConflictException 직접 입력한 코드가 이미 있을 때
   */
  public String resolve(String requested) {
    if (requested != null && !requested.isBlank()) {
      if (products.existsByCodeIncludingDeleted(requested)) {
        throw new ConflictException(
            "이미 사용 중인 상품 코드입니다: " + requested + ". 다른 코드를 입력하거나 비워 두면 자동으로 만들어 집니다.");
      }
      return requested;
    }
    // 직접 입력한 코드가 시퀀스 값과 겹칠 수 있으므로 비어 있는 값을 찾을 때까지 건너뛴다.
    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      String candidate = format(products.nextCodeSequence());
      if (!products.existsByCodeIncludingDeleted(candidate)) {
        return candidate;
      }
    }
    throw new IllegalStateException("상품 코드를 자동으로 만들지 못했습니다. 잠시 후 다시 시도하세요.");
  }
}
