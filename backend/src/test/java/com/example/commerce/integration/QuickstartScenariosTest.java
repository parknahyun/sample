package com.example.commerce.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** quickstart.md의 검증 시나리오가 모두 자동 테스트에 대응하는지 확인하는 추적 테스트. 시나리오를 추가하고 대응 테스트를 만들지 않으면 이 테스트가 실패한다. */
class QuickstartScenariosTest {

  private static final String PKG = "com.example.commerce.";

  /** 시나리오 번호 → 검증하는 테스트 메서드(클래스#메서드). */
  private static final Map<String, List<String>> COVERAGE = new LinkedHashMap<>();

  static {
    String reg = "integration.ProductRegistrationIntegrationTest#";
    String img = "integration.ProductImageUploadIntegrationTest#";
    String manage = "integration.ProductImageManageIntegrationTest#";
    String upd = "integration.ProductUpdateIntegrationTest#";
    String search = "integration.ProductSearchIntegrationTest#";
    String del = "integration.ProductDeleteIntegrationTest#";
    String store = "integration.StorefrontIntegrationTest#";
    String sec = "integration.SecurityIntegrationTest#";
    COVERAGE.put("1", List.of(reg + "등록하면_코드가_PRD로_자동_생성되고_목록과_상세에서_조회된다"));
    COVERAGE.put("2", List.of(reg + "필수_항목을_비우면_400과_누락된_항목을_안내한다"));
    COVERAGE.put("3", List.of(reg + "가격_0과_재고_음수는_거부한다"));
    COVERAGE.put("4", List.of(reg + "직접_입력한_코드를_쓰고_중복이면_409"));
    COVERAGE.put(
        "5", List.of(reg + "옵션_상품은_조합별_재고로_등록되고_상품_재고는_null이다", reg + "같은_옵션_조합을_중복으로_정의하면_400"));
    COVERAGE.put(
        "6",
        List.of(img + "상품당_10장까지만_올릴_수_있다", img + "오메가바이트_초과_파일은_413", img + "GIF와_위장_파일은_거부한다"));
    COVERAGE.put(
        "6-1", List.of(img + "이미지_없이_등록한_상품도_허용한다", manage + "대표_이미지를_삭제하면_남은_첫_이미지가_대표가_된다"));
    COVERAGE.put("7", List.of(upd + "모든_옵션_조합의_재고가_0이면_품절이_되고_보충하면_판매중으로_자동_복귀한다"));
    COVERAGE.put("8", List.of(upd + "모든_옵션_조합의_재고가_0이면_품절이_되고_보충하면_판매중으로_자동_복귀한다"));
    COVERAGE.put("9", List.of(upd + "숨김_상품은_재고를_보충해도_숨김이_유지된다"));
    COVERAGE.put("10", List.of(upd + "같은_버전으로_두_번_저장하면_두_번째는_409"));
    COVERAGE.put(
        "11",
        List.of(
            search + "카테고리와_판매_상태로_거르고_조합할_수_있다",
            search + "등록일과_가격으로_정렬한다",
            search + "페이지로_나누고_범위를_벗어난_페이지는_빈_목록이다"));
    COVERAGE.put(
        "12", List.of("integration.ProductSearchPerformanceTest#상품_1만_개에서_검색_응답은_p95_1초_미만이다"));
    COVERAGE.put("13", List.of(del + "삭제하면_목록에서_사라지고_상세는_404이며_데이터는_보존된다"));
    COVERAGE.put("14", List.of(del + "주문_이력이_있는_상품을_삭제해도_과거_주문_정보가_보존된다"));
    COVERAGE.put(
        "15",
        List.of(
            reg + "등록하면_CREATED_변경이력이_기록된다",
            upd + "수정하면_UPDATED_이력에_변경_필드와_이전_이후_값이_기록된다",
            del + "삭제는_DELETED_변경이력으로_기록된다"));
    COVERAGE.put("16", List.of(sec + "인증_없이_운영자_API를_호출하면_401", sec + "다른_역할로_운영자_API를_호출하면_403"));
    COVERAGE.put(
        "17", List.of(store + "판매중과_품절만_노출하고_숨김은_제외한다", store + "삭제된_상품은_목록에서_제외되고_상세는_404다"));
    COVERAGE.put("18", List.of(store + "수정은_고객_조회에_즉시_반영된다"));
  }

  private static Set<String> scenarioIdsInQuickstart() throws IOException {
    Path contract = Path.of(System.getProperty("contract.file"));
    Path quickstart = contract.getParent().getParent().resolve("quickstart.md");
    Pattern row = Pattern.compile("^\\| (\\d+(?:-\\d+)?) \\|");
    Set<String> ids = new LinkedHashSet<>();
    for (String line : Files.readAllLines(quickstart)) {
      Matcher matcher = row.matcher(line);
      if (matcher.find()) {
        ids.add(matcher.group(1));
      }
    }
    return ids;
  }

  @Test
  void quickstart의_모든_시나리오에_대응하는_테스트가_있다() throws IOException {
    assertThat(scenarioIdsInQuickstart())
        .as("quickstart.md의 시나리오 번호와 이 테스트의 대응표가 일치해야 한다")
        .containsExactlyInAnyOrderElementsOf(COVERAGE.keySet());
  }

  @Test
  void 대응표의_모든_테스트_메서드가_실제로_존재한다() throws Exception {
    for (Map.Entry<String, List<String>> entry : COVERAGE.entrySet()) {
      for (String target : entry.getValue()) {
        String[] parts = target.split("#");
        Class<?> type = Class.forName(PKG + parts[0]);
        boolean exists =
            Arrays.stream(type.getDeclaredMethods())
                .map(Method::getName)
                .anyMatch(parts[1]::equals);
        assertThat(exists).as("시나리오 %s의 테스트 %s", entry.getKey(), target).isTrue();
      }
    }
  }
}
