package com.example.commerce.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.commerce.product.domain.VariantCombinationKey;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** FR-015: 옵션 값 조합을 정규화한 키를 만들고, 모든 조합이 빠짐없이 있는지 검출한다. */
class VariantCombinationKeyTest {

  private static Map<String, List<String>> groups() {
    Map<String, List<String>> groups = new LinkedHashMap<>();
    groups.put("색상", List.of("빨강", "파랑"));
    groups.put("사이즈", List.of("M", "L"));
    return groups;
  }

  @Test
  void 옵션_그룹_순서대로_키를_만든다() {
    // 입력 맵의 순서와 무관하게 옵션 그룹 순서를 따른다.
    Map<String, String> values = new LinkedHashMap<>();
    values.put("사이즈", "M");
    values.put("색상", "빨강");

    assertThat(VariantCombinationKey.key(List.of("색상", "사이즈"), values)).isEqualTo("색상=빨강|사이즈=M");
  }

  @Test
  void 옵션_그룹이_빠진_조합은_키를_만들_수_없다() {
    assertThatThrownBy(() -> VariantCombinationKey.key(List.of("색상", "사이즈"), Map.of("색상", "빨강")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 모든_옵션_조합을_만든다() {
    assertThat(VariantCombinationKey.allKeys(groups()))
        .containsExactlyInAnyOrder("색상=빨강|사이즈=M", "색상=빨강|사이즈=L", "색상=파랑|사이즈=M", "색상=파랑|사이즈=L");
  }

  @Test
  void 누락된_조합과_알_수_없는_조합을_검출한다() {
    List<String> provided = List.of("색상=빨강|사이즈=M", "색상=파랑|사이즈=M", "색상=초록|사이즈=M");

    assertThat(VariantCombinationKey.missing(groups(), provided))
        .containsExactlyInAnyOrder("색상=빨강|사이즈=L", "색상=파랑|사이즈=L");
    assertThat(VariantCombinationKey.unknown(groups(), provided)).containsExactly("색상=초록|사이즈=M");
  }

  @Test
  void 키를_옵션_이름과_값으로_되돌린다() {
    assertThat(VariantCombinationKey.parse("색상=빨강|사이즈=M"))
        .containsExactly(Map.entry("색상", "빨강"), Map.entry("사이즈", "M"));
  }

  @Test
  void 옵션_이름과_값에는_구분자를_쓸_수_없다() {
    assertThat(VariantCombinationKey.isValidToken("빨강")).isTrue();
    assertThat(VariantCombinationKey.isValidToken("a=b")).isFalse();
    assertThat(VariantCombinationKey.isValidToken("a|b")).isFalse();
    assertThat(VariantCombinationKey.isValidToken(" ")).isFalse();
  }
}
