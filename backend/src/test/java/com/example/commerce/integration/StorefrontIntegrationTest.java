package com.example.commerce.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.example.commerce.support.TestImages;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/** FR-018: 고객 노출용 조회(quickstart 시나리오 17~18). 판매중·품절만 노출하고 숨김·삭제는 제외한다. */
class StorefrontIntegrationTest extends IntegrationTestBase {

  private JsonNode list() throws Exception {
    return json(mvc.perform(get("/api/v1/storefront/products")).andReturn());
  }

  private int detailStatus(long id) throws Exception {
    return mvc.perform(get("/api/v1/storefront/products/{id}", id))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  private Map<String, Object> withStatus(String name, String status, int stock) {
    Map<String, Object> body = productBody(name);
    body.put("status", status);
    body.put("stockQuantity", stock);
    return body;
  }

  @Test
  void 판매중과_품절만_노출하고_숨김은_제외한다() throws Exception {
    long onSale = createProduct(withStatus("판매중", "ON_SALE", 5)).get("id").asLong();
    long soldOut = createProduct(withStatus("품절", "ON_SALE", 0)).get("id").asLong();
    long hidden = createProduct(withStatus("숨김", "HIDDEN", 5)).get("id").asLong();

    JsonNode content = list().get("content");

    assertThat(content.findValuesAsText("name")).containsExactlyInAnyOrder("판매중", "품절");
    assertThat(detailStatus(onSale)).isEqualTo(200);
    assertThat(detailStatus(soldOut)).isEqualTo(200);
    assertThat(detailStatus(hidden)).isEqualTo(404);
  }

  @Test
  void 삭제된_상품은_목록에서_제외되고_상세는_404다() throws Exception {
    long id = createProduct(withStatus("삭제될 상품", "ON_SALE", 5)).get("id").asLong();
    // 삭제 API는 사용자 스토리 4에서 구현하므로, 여기서는 삭제 표시(deleted_at)를 직접 넣어 노출 규칙만 검증한다.
    jdbc.update("update products set deleted_at = now() where id = ?", id);

    assertThat(list().get("content")).isEmpty();
    assertThat(detailStatus(id)).isEqualTo(404);
  }

  @Test
  void 응답에_재고_수량_코드_버전이_없고_품절_여부만_있다() throws Exception {
    long soldOut = createProduct(withStatus("품절 상품", "ON_SALE", 0)).get("id").asLong();
    long onSale = createProduct(withStatus("판매 상품", "ON_SALE", 9)).get("id").asLong();

    JsonNode soldOutDetail =
        json(mvc.perform(get("/api/v1/storefront/products/{id}", soldOut)).andReturn());
    JsonNode onSaleDetail =
        json(mvc.perform(get("/api/v1/storefront/products/{id}", onSale)).andReturn());

    assertThat(soldOutDetail.get("soldOut").asBoolean()).isTrue();
    assertThat(onSaleDetail.get("soldOut").asBoolean()).isFalse();
    for (JsonNode detail : List.of(soldOutDetail, onSaleDetail)) {
      assertThat(detail.has("stockQuantity")).isFalse();
      assertThat(detail.has("code")).isFalse();
      assertThat(detail.has("version")).isFalse();
      assertThat(detail.has("status")).isFalse();
    }
  }

  @Test
  void 옵션_조합은_품절_여부만_노출한다() throws Exception {
    Map<String, Object> body = productBody("옵션 노출");
    body.remove("stockQuantity");
    body.put("optionGroups", List.of(Map.of("name", "사이즈", "values", List.of("M", "L"))));
    body.put(
        "variants",
        List.of(
            Map.of("optionValues", Map.of("사이즈", "M"), "stockQuantity", 3),
            Map.of("optionValues", Map.of("사이즈", "L"), "stockQuantity", 0)));
    long id = createProduct(body).get("id").asLong();

    JsonNode detail = json(mvc.perform(get("/api/v1/storefront/products/{id}", id)).andReturn());

    assertThat(detail.get("soldOut").asBoolean()).isFalse();
    JsonNode variants = detail.get("variants");
    assertThat(variants).hasSize(2);
    assertThat(variants.get(0).get("soldOut").asBoolean()).isFalse();
    assertThat(variants.get(1).get("soldOut").asBoolean()).isTrue();
    assertThat(variants.get(0).has("stockQuantity")).isFalse();
  }

  @Test
  void 수정은_고객_조회에_즉시_반영된다() throws Exception {
    JsonNode product = createProduct(withStatus("가격 변경", "ON_SALE", 5));
    long id = product.get("id").asLong();
    Map<String, Object> body = new LinkedHashMap<>(withStatus("가격 변경", "ON_SALE", 5));
    body.put("price", 33000);
    body.put("version", product.get("version").asLong());

    mvc.perform(
        put("/api/v1/products/{id}", id)
            .with(manager())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)));

    JsonNode detail = json(mvc.perform(get("/api/v1/storefront/products/{id}", id)).andReturn());
    assertThat(detail.get("price").asLong()).isEqualTo(33000);
  }

  @Test
  void 숨김으로_바꾸면_고객_목록에서_바로_사라진다() throws Exception {
    JsonNode product = createProduct(withStatus("숨김 전환", "ON_SALE", 5));
    long id = product.get("id").asLong();
    assertThat(detailStatus(id)).isEqualTo(200);
    Map<String, Object> body = new LinkedHashMap<>(withStatus("숨김 전환", "HIDDEN", 5));
    body.put("version", product.get("version").asLong());

    mvc.perform(
        put("/api/v1/products/{id}", id)
            .with(manager())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)));

    assertThat(list().get("content")).isEmpty();
    assertThat(detailStatus(id)).isEqualTo(404);
  }

  @Test
  void 카테고리로_좁히고_페이지로_나눈다() throws Exception {
    long other =
        jdbc.queryForObject("insert into categories (name) values ('기타') returning id", Long.class);
    createProduct(productBody("기본 카테고리 상품"));
    Map<String, Object> inOther = productBody("기타 상품");
    inOther.put("categoryId", other);
    createProduct(inOther);

    MvcResult filtered =
        mvc.perform(get("/api/v1/storefront/products").param("categoryId", String.valueOf(other)))
            .andReturn();
    assertThat(json(filtered).get("content").findValuesAsText("name")).containsExactly("기타 상품");

    JsonNode firstPage =
        json(
            mvc.perform(get("/api/v1/storefront/products").param("size", "1").param("page", "0"))
                .andReturn());
    assertThat(firstPage.get("content")).hasSize(1);
    assertThat(firstPage.get("totalElements").asLong()).isEqualTo(2);
    assertThat(firstPage.get("totalPages").asInt()).isEqualTo(2);
  }

  @Test
  void 이미지_주소가_대표_이미지로_함께_내려온다() throws Exception {
    long id = createProduct(withStatus("이미지 노출", "ON_SALE", 5)).get("id").asLong();
    String url = json(uploadImage(id, TestImages.png(100), "a.png")).get("url").asText();

    JsonNode detail = json(mvc.perform(get("/api/v1/storefront/products/{id}", id)).andReturn());

    assertThat(detail.get("images")).hasSize(1);
    assertThat(detail.get("images").get(0).get("url").asText()).isEqualTo(url);
  }
}
