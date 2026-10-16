package com.example.commerce.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.commerce.product.service.ProductCodeGenerator;
import org.junit.jupiter.api.Test;

/** FR-005: 상품 코드는 PRD-000001 형식으로 자동 생성하고, 직접 입력한 코드는 형식을 검증한다. */
class ProductCodeGeneratorTest {

  @Test
  void 시퀀스_값을_PRD_여섯자리_형식으로_만든다() {
    assertThat(ProductCodeGenerator.format(1)).isEqualTo("PRD-000001");
    assertThat(ProductCodeGenerator.format(123456)).isEqualTo("PRD-123456");
  }

  @Test
  void 자동_생성한_코드는_허용_형식을_만족한다() {
    assertThat(ProductCodeGenerator.isValidFormat(ProductCodeGenerator.format(42))).isTrue();
  }

  @Test
  void 영문_대문자_숫자_하이픈_3에서_30자만_허용한다() {
    assertThat(ProductCodeGenerator.isValidFormat("ABC")).isTrue();
    assertThat(ProductCodeGenerator.isValidFormat("SHOE-2026-RED")).isTrue();
    assertThat(ProductCodeGenerator.isValidFormat("A".repeat(30))).isTrue();
  }

  @Test
  void 형식에_맞지_않는_코드는_거부한다() {
    assertThat(ProductCodeGenerator.isValidFormat("AB")).isFalse();
    assertThat(ProductCodeGenerator.isValidFormat("A".repeat(31))).isFalse();
    assertThat(ProductCodeGenerator.isValidFormat("abc-123")).isFalse();
    assertThat(ProductCodeGenerator.isValidFormat("ABC 123")).isFalse();
    assertThat(ProductCodeGenerator.isValidFormat("ABC_123")).isFalse();
    assertThat(ProductCodeGenerator.isValidFormat("")).isFalse();
    assertThat(ProductCodeGenerator.isValidFormat(null)).isFalse();
  }
}
