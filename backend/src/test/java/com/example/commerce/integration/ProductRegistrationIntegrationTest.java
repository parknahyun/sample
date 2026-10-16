package com.example.commerce.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** 사용자 스토리 1: 상품 등록(quickstart 시나리오 1~5). */
class ProductRegistrationIntegrationTest extends IntegrationTestBase {

  @Test
  void 등록하면_코드가_PRD로_자동_생성되고_목록과_상세에서_조회된다() throws Exception {
    MvcResult created =
        mvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                        "/api/v1/products")
                    .with(manager())
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productBody("면 티셔츠"))))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", matchesPattern(".*/api/v1/products/\\d+")))
            .andExpect(jsonPath("$.code", matchesPattern("PRD-\\d{6}")))
            .andExpect(jsonPath("$.status").value("ON_SALE"))
            .andExpect(jsonPath("$.stockQuantity").value(5))
            .andReturn();
    long id = json(created).get("id").asLong();

    mvc.perform(get("/api/v1/products/{id}", id).with(manager()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("면 티셔츠"))
        .andExpect(jsonPath("$.price").value(10000))
        .andExpect(jsonPath("$.images").isEmpty());

    mvc.perform(get("/api/v1/products").with(manager()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("면 티셔츠"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void 필수_항목을_비우면_400과_누락된_항목을_안내한다() throws Exception {
    MvcResult result = postProduct(new LinkedHashMap<>());

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    JsonNode errors = json(result).get("errors");
    List<String> fields = new ArrayList<>();
    errors.forEach(e -> fields.add(e.get("field").asText()));
    assertThat(fields).contains("name", "price", "categoryId");
    assertThat(errors.toString()).contains("상품명은 필수", "가격은 필수", "카테고리는 필수");
  }

  @Test
  void 가격_0과_재고_음수는_거부한다() throws Exception {
    Map<String, Object> body = productBody("잘못된 상품");
    body.put("price", 0);
    body.put("stockQuantity", -1);

    MvcResult result = postProduct(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(json(result).get("errors").toString())
        .contains("\"field\":\"price\"", "0보다 큰", "\"field\":\"stockQuantity\"", "0 이상");
    assertThat(jdbc.queryForObject("select count(*) from products", Long.class)).isZero();
  }

  @Test
  void 숫자가_아닌_가격은_요청_형식_오류로_400() throws Exception {
    Map<String, Object> body = productBody("문자 가격");
    body.put("price", "만원");

    assertThat(postProduct(body).getResponse().getStatus()).isEqualTo(400);
  }

  @Test
  void 직접_입력한_코드를_쓰고_중복이면_409() throws Exception {
    Map<String, Object> body = productBody("첫 상품");
    body.put("code", "SHOE-001");
    assertThat(createProduct(body).get("code").asText()).isEqualTo("SHOE-001");

    Map<String, Object> duplicate = productBody("둘째 상품");
    duplicate.put("code", "SHOE-001");
    MvcResult result = postProduct(duplicate);

    assertThat(result.getResponse().getStatus()).isEqualTo(409);
    assertThat(json(result).get("detail").asText()).contains("SHOE-001", "이미 사용 중");
  }

  @Test
  void 소문자_코드는_형식_오류로_400() throws Exception {
    Map<String, Object> body = productBody("소문자 코드");
    body.put("code", "shoe-001");

    MvcResult result = postProduct(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(result.getResponse().getContentAsString()).contains("code");
  }

  @Test
  void 존재하지_않는_카테고리는_400() throws Exception {
    Map<String, Object> body = productBody("없는 카테고리");
    body.put("categoryId", 999999);

    MvcResult result = postProduct(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(json(result).get("errors").toString()).contains("categoryId", "존재하지 않는 카테고리");
  }

  @Test
  void 옵션_상품은_조합별_재고로_등록되고_상품_재고는_null이다() throws Exception {
    JsonNode product = createProduct(optionBody("티셔츠", 3, 2, 0, 4));

    assertThat(product.get("stockQuantity").isNull()).isTrue();
    assertThat(product.get("variants")).hasSize(4);
    assertThat(product.get("optionGroups")).hasSize(2);
    assertThat(product.get("status").asText()).isEqualTo("ON_SALE");
  }

  @Test
  void 모든_옵션_조합의_재고가_0이면_품절이고_일부만_0이면_판매중이다() throws Exception {
    assertThat(createProduct(optionBody("전부 품절", 0, 0, 0, 0)).get("status").asText())
        .isEqualTo("SOLD_OUT");
    assertThat(createProduct(optionBody("일부 품절", 0, 3, 0, 0)).get("status").asText())
        .isEqualTo("ON_SALE");
  }

  @Test
  void 옵션이_없는_상품은_재고_0이면_품절이고_숨김은_유지된다() throws Exception {
    Map<String, Object> soldOut = productBody("재고 없음");
    soldOut.put("stockQuantity", 0);
    assertThat(createProduct(soldOut).get("status").asText()).isEqualTo("SOLD_OUT");

    Map<String, Object> hidden = productBody("숨김 상품");
    hidden.put("status", "HIDDEN");
    hidden.put("stockQuantity", 0);
    assertThat(createProduct(hidden).get("status").asText()).isEqualTo("HIDDEN");
  }

  @Test
  void 품절은_운영자가_직접_지정할_수_없다() throws Exception {
    Map<String, Object> body = productBody("품절 지정");
    body.put("status", "SOLD_OUT");

    MvcResult result = postProduct(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(json(result).get("errors").toString()).contains("status", "재고로 결정");
  }

  @Test
  void 같은_옵션_조합을_중복으로_정의하면_400() throws Exception {
    Map<String, Object> body = optionBody("중복 조합", 1, 1, 1, 1);
    List<Map<String, Object>> variants = variants(body);
    variants.set(3, new LinkedHashMap<>(variants.get(0)));

    MvcResult result = postProduct(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(json(result).get("errors").toString()).contains("variants", "중복");
  }

  @Test
  void 옵션_조합이_누락되면_400() throws Exception {
    Map<String, Object> body = optionBody("누락 조합", 1, 1, 1, 1);
    variants(body).remove(3);

    MvcResult result = postProduct(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(json(result).get("errors").toString()).contains("variants", "모든 옵션 조합");
  }

  @Test
  void 옵션이_없는데_옵션_조합을_보내면_400() throws Exception {
    Map<String, Object> body = productBody("조합만 있음");
    body.put("variants", List.of(variant("색상", "빨강", "사이즈", "M", 1)));

    assertThat(postProduct(body).getResponse().getStatus()).isEqualTo(400);
  }

  @Test
  void 등록하면_CREATED_변경이력이_기록된다() throws Exception {
    JsonNode product = createProduct(productBody("이력 확인"));

    Map<String, Object> row =
        jdbc.queryForMap(
            "select action, changed_by, changed_at, summary::text as summary from product_change_logs"
                + " where product_id = ?",
            product.get("id").asLong());
    assertThat(row.get("action")).isEqualTo("CREATED");
    assertThat(row.get("changed_by")).isEqualTo("tester");
    assertThat(row.get("changed_at")).isNotNull();
    assertThat((String) row.get("summary")).contains("이력 확인");
  }

  @Test
  void 존재하지_않는_상품_조회는_404() throws Exception {
    mvc.perform(get("/api/v1/products/{id}", 987654).with(manager()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail", containsString("존재하지 않는 상품")));
  }

  // --- 테스트 데이터 도우미 ---

  /** 색상(빨강·파랑) × 사이즈(M·L) 옵션 상품. 재고는 (빨강M, 빨강L, 파랑M, 파랑L) 순서다. */
  private Map<String, Object> optionBody(String name, int redM, int redL, int blueM, int blueL) {
    Map<String, Object> body = productBody(name);
    body.remove("stockQuantity");
    body.put(
        "optionGroups",
        List.of(
            Map.of("name", "색상", "values", List.of("빨강", "파랑")),
            Map.of("name", "사이즈", "values", List.of("M", "L"))));
    List<Map<String, Object>> variants = new ArrayList<>();
    variants.add(variant("색상", "빨강", "사이즈", "M", redM));
    variants.add(variant("색상", "빨강", "사이즈", "L", redL));
    variants.add(variant("색상", "파랑", "사이즈", "M", blueM));
    variants.add(variant("색상", "파랑", "사이즈", "L", blueL));
    body.put("variants", variants);
    return body;
  }

  private Map<String, Object> variant(String g1, String v1, String g2, String v2, int stock) {
    Map<String, Object> optionValues = new LinkedHashMap<>();
    optionValues.put(g1, v1);
    optionValues.put(g2, v2);
    Map<String, Object> variant = new LinkedHashMap<>();
    variant.put("optionValues", optionValues);
    variant.put("stockQuantity", stock);
    return variant;
  }

  // body.get("variants")는 이 테스트 파일이 직접 만든 List<Map>이므로 형 변환이 안전하다.
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> variants(Map<String, Object> body) {
    return (List<Map<String, Object>>) body.get("variants");
  }
}
