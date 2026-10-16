package com.example.commerce.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/** 사용자 스토리 2: 상품 수정과 판매 상태 관리(quickstart 시나리오 7~10). */
class ProductUpdateIntegrationTest extends IntegrationTestBase {

  private MvcResult putProduct(long id, Map<String, Object> body) throws Exception {
    return mvc.perform(
            put("/api/v1/products/{id}", id)
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andReturn();
  }

  /** 상세 조회 응답을 수정 요청 본문으로 바꾼다(편집 화면이 하는 일). */
  private Map<String, Object> editBody(JsonNode product) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("name", product.get("name").asText());
    body.put("price", product.get("price").asLong());
    body.put("categoryId", product.get("categoryId").asLong());
    body.put("status", product.get("status").asText().equals("HIDDEN") ? "HIDDEN" : "ON_SALE");
    body.put("version", product.get("version").asLong());
    if (!product.get("stockQuantity").isNull()) {
      body.put("stockQuantity", product.get("stockQuantity").asInt());
    }
    if (product.get("optionGroups").size() > 0) {
      body.put("optionGroups", objectMapper.convertValue(product.get("optionGroups"), List.class));
      body.put("variants", objectMapper.convertValue(product.get("variants"), List.class));
    }
    return body;
  }

  private JsonNode fetch(long id) throws Exception {
    return json(mvc.perform(get("/api/v1/products/{id}", id).with(manager())).andReturn());
  }

  private Map<String, Object> variant(String size, int stock) {
    Map<String, Object> variant = new LinkedHashMap<>();
    variant.put("optionValues", Map.of("사이즈", size));
    variant.put("stockQuantity", stock);
    return variant;
  }

  private JsonNode createSizedProduct(int stockM, int stockL) throws Exception {
    Map<String, Object> body = productBody("옵션 상품");
    body.remove("stockQuantity");
    body.put("optionGroups", List.of(Map.of("name", "사이즈", "values", List.of("M", "L"))));
    body.put("variants", List.of(variant("M", stockM), variant("L", stockL)));
    return createProduct(body);
  }

  @Test
  void 가격을_수정하면_이후_조회에_새_가격이_보이고_버전이_오른다() throws Exception {
    JsonNode product = createProduct(productBody("수정 대상"));
    Map<String, Object> body = editBody(product);
    body.put("price", 25000);
    body.put("description", "새 설명");

    MvcResult result = putProduct(product.get("id").asLong(), body);

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    JsonNode updated = fetch(product.get("id").asLong());
    assertThat(updated.get("price").asLong()).isEqualTo(25000);
    assertThat(updated.get("description").asText()).isEqualTo("새 설명");
    assertThat(updated.get("version").asLong()).isGreaterThan(product.get("version").asLong());
    assertThat(updated.get("code").asText()).isEqualTo(product.get("code").asText());
  }

  @Test
  void 모든_옵션_조합의_재고가_0이면_품절이_되고_보충하면_판매중으로_자동_복귀한다() throws Exception {
    JsonNode product = createSizedProduct(3, 2);
    long id = product.get("id").asLong();

    Map<String, Object> soldOut = editBody(product);
    soldOut.put("variants", List.of(variant("M", 0), variant("L", 0)));
    assertThat(json(putProduct(id, soldOut)).get("status").asText()).isEqualTo("SOLD_OUT");

    Map<String, Object> restocked = editBody(fetch(id));
    restocked.put("variants", List.of(variant("M", 0), variant("L", 4)));
    JsonNode restockedProduct = json(putProduct(id, restocked));
    assertThat(restockedProduct.get("status").asText()).isEqualTo("ON_SALE");
    // 일부 옵션 조합만 재고 0이면 상품은 판매중이고 해당 조합만 품절(재고 0)로 표시된다.
    assertThat(restockedProduct.get("variants").findValuesAsText("stockQuantity"))
        .containsExactly("0", "4");
  }

  @Test
  void 숨김_상품은_재고를_보충해도_숨김이_유지된다() throws Exception {
    Map<String, Object> hidden = productBody("숨길 상품");
    hidden.put("status", "HIDDEN");
    hidden.put("stockQuantity", 0);
    JsonNode product = createProduct(hidden);

    Map<String, Object> restocked = editBody(product);
    restocked.put("stockQuantity", 10);

    JsonNode updated = json(putProduct(product.get("id").asLong(), restocked));
    assertThat(updated.get("status").asText()).isEqualTo("HIDDEN");
  }

  @Test
  void 숨김을_해제하면_재고에_따라_판매중_또는_품절이_된다() throws Exception {
    Map<String, Object> hidden = productBody("해제 상품");
    hidden.put("status", "HIDDEN");
    hidden.put("stockQuantity", 0);
    JsonNode product = createProduct(hidden);

    Map<String, Object> unhide = editBody(product);
    unhide.put("status", "ON_SALE");

    assertThat(json(putProduct(product.get("id").asLong(), unhide)).get("status").asText())
        .isEqualTo("SOLD_OUT");
  }

  @Test
  void 같은_버전으로_두_번_저장하면_두_번째는_409() throws Exception {
    JsonNode product = createProduct(productBody("동시 수정"));
    long id = product.get("id").asLong();
    Map<String, Object> first = editBody(product);
    first.put("name", "먼저 저장");
    Map<String, Object> second = editBody(product);
    second.put("name", "나중 저장");

    assertThat(putProduct(id, first).getResponse().getStatus()).isEqualTo(200);
    MvcResult conflict = putProduct(id, second);

    assertThat(conflict.getResponse().getStatus()).isEqualTo(409);
    assertThat(json(conflict).get("detail").asText()).contains("최신 내용을 다시 불러");
    assertThat(fetch(id).get("name").asText()).isEqualTo("먼저 저장");
  }

  @Test
  void 수정_요청은_상품_코드를_바꾸지_못한다() throws Exception {
    JsonNode product = createProduct(productBody("코드 고정"));
    Map<String, Object> body = editBody(product);
    body.put("code", "HACK-001");

    putProduct(product.get("id").asLong(), body);

    assertThat(fetch(product.get("id").asLong()).get("code").asText())
        .isEqualTo(product.get("code").asText());
  }

  @Test
  void 옵션_정의를_바꾸면_옵션_조합이_다시_만들어진다() throws Exception {
    JsonNode product = createSizedProduct(1, 1);
    Map<String, Object> body = editBody(product);
    body.put("optionGroups", List.of(Map.of("name", "사이즈", "values", List.of("M", "L", "XL"))));
    List<Map<String, Object>> variants = new ArrayList<>();
    variants.add(variant("M", 1));
    variants.add(variant("L", 1));
    variants.add(variant("XL", 9));
    body.put("variants", variants);

    JsonNode updated = json(putProduct(product.get("id").asLong(), body));

    assertThat(updated.get("variants")).hasSize(3);
    assertThat(updated.get("optionGroups").get(0).get("values")).hasSize(3);
  }

  @Test
  void 옵션을_모두_없애면_상품_재고를_쓴다() throws Exception {
    JsonNode product = createSizedProduct(1, 1);
    Map<String, Object> body = editBody(product);
    body.remove("optionGroups");
    body.remove("variants");
    body.put("stockQuantity", 7);

    JsonNode updated = json(putProduct(product.get("id").asLong(), body));

    assertThat(updated.get("optionGroups")).isEmpty();
    assertThat(updated.get("stockQuantity").asInt()).isEqualTo(7);
  }

  @Test
  void 검증_실패는_400이고_상품은_바뀌지_않는다() throws Exception {
    JsonNode product = createProduct(productBody("검증 상품"));
    Map<String, Object> body = editBody(product);
    body.put("price", 0);

    MvcResult result = putProduct(product.get("id").asLong(), body);

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(fetch(product.get("id").asLong()).get("price").asLong()).isEqualTo(10000);
  }

  @Test
  void 버전이_없으면_400() throws Exception {
    JsonNode product = createProduct(productBody("버전 필수"));
    Map<String, Object> body = editBody(product);
    body.remove("version");

    MvcResult result = putProduct(product.get("id").asLong(), body);

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(json(result).get("errors").toString()).contains("version");
  }

  @Test
  void 존재하지_않는_상품_수정은_404() throws Exception {
    Map<String, Object> body = productBody("없는 상품");
    body.put("version", 0);

    assertThat(putProduct(424242, body).getResponse().getStatus()).isEqualTo(404);
  }

  @Test
  void 수정하면_UPDATED_이력에_변경_필드와_이전_이후_값이_기록된다() throws Exception {
    JsonNode product = createProduct(productBody("이력 상품"));
    Map<String, Object> body = editBody(product);
    body.put("price", 12345);
    putProduct(product.get("id").asLong(), body);

    List<Map<String, Object>> logs =
        jdbc.queryForList(
            "select action, changed_by, summary::text as summary from product_change_logs"
                + " where product_id = ? order by id",
            product.get("id").asLong());

    assertThat(logs).extracting(l -> l.get("action")).containsExactly("CREATED", "UPDATED");
    String summary = (String) logs.get(1).get("summary");
    assertThat(summary).contains("price", "10000", "12345");
    assertThat(summary).doesNotContain("\"name\"");
  }
}
