package com.example.commerce.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.example.commerce.support.TestImages;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/** 사용자 스토리 4: 상품 삭제(quickstart 시나리오 13~15). 삭제는 논리 삭제이며 과거 주문 정보는 보존된다. */
class ProductDeleteIntegrationTest extends IntegrationTestBase {

  private MvcResult deleteProduct(long id) throws Exception {
    return mvc.perform(delete("/api/v1/products/{id}", id).with(manager())).andReturn();
  }

  private int status(MvcResult result) {
    return result.getResponse().getStatus();
  }

  @Test
  void 삭제하면_목록에서_사라지고_상세는_404이며_데이터는_보존된다() throws Exception {
    long id = createProduct(productBody("삭제 대상")).get("id").asLong();

    assertThat(status(deleteProduct(id))).isEqualTo(204);

    assertThat(
            json(mvc.perform(get("/api/v1/products").with(manager())).andReturn()).get("content"))
        .isEmpty();
    assertThat(status(mvc.perform(get("/api/v1/products/{id}", id).with(manager())).andReturn()))
        .isEqualTo(404);
    Map<String, Object> row =
        jdbc.queryForMap("select name, deleted_at from products where id = ?", id);
    assertThat(row.get("name")).isEqualTo("삭제 대상");
    assertThat(row.get("deleted_at")).isNotNull();
  }

  @Test
  void 주문_이력이_있는_상품을_삭제해도_과거_주문_정보가_보존된다() throws Exception {
    JsonNode product = createProduct(productBody("주문된 상품"));
    long id = product.get("id").asLong();
    jdbc.update(
        "insert into test_order_item (product_id, product_name, unit_price) values (?, ?, ?)",
        id,
        "주문된 상품",
        10000);

    assertThat(status(deleteProduct(id))).isEqualTo(204);

    Map<String, Object> order =
        jdbc.queryForMap(
            "select o.product_name, o.unit_price, p.name as current_name, p.price as current_price"
                + " from test_order_item o join products p on p.id = o.product_id"
                + " where o.product_id = ?",
            id);
    assertThat(order.get("product_name")).isEqualTo("주문된 상품");
    assertThat(((Number) order.get("unit_price")).longValue()).isEqualTo(10000);
    assertThat(order.get("current_name")).isEqualTo("주문된 상품");
    assertThat(jdbc.queryForObject("select count(*) from test_order_item", Long.class))
        .isEqualTo(1);
  }

  @Test
  void 옵션과_이미지_메타데이터도_보존된다() throws Exception {
    Map<String, Object> body = productBody("옵션 이미지 상품");
    body.remove("stockQuantity");
    body.put("optionGroups", List.of(Map.of("name", "사이즈", "values", List.of("M", "L"))));
    body.put(
        "variants",
        List.of(
            Map.of("optionValues", Map.of("사이즈", "M"), "stockQuantity", 1),
            Map.of("optionValues", Map.of("사이즈", "L"), "stockQuantity", 2)));
    long id = createProduct(body).get("id").asLong();
    uploadImage(id, TestImages.png(100), "a.png");

    deleteProduct(id);

    assertThat(jdbc.queryForObject("select count(*) from product_variants", Long.class))
        .isEqualTo(2);
    assertThat(jdbc.queryForObject("select count(*) from product_option_groups", Long.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("select count(*) from product_option_values", Long.class))
        .isEqualTo(2);
    assertThat(jdbc.queryForObject("select count(*) from product_images", Long.class)).isEqualTo(1);
  }

  @Test
  void 삭제된_상품의_코드로_다시_등록하면_409() throws Exception {
    Map<String, Object> body = productBody("코드 재사용");
    body.put("code", "REUSE-001");
    long id = createProduct(body).get("id").asLong();
    deleteProduct(id);

    Map<String, Object> again = productBody("같은 코드");
    again.put("code", "REUSE-001");
    MvcResult result = postProduct(again);

    assertThat(status(result)).isEqualTo(409);
    assertThat(json(result).get("detail").asText()).contains("REUSE-001");
  }

  @Test
  void 삭제는_DELETED_변경이력으로_기록된다() throws Exception {
    long id = createProduct(productBody("이력 삭제")).get("id").asLong();

    deleteProduct(id);

    List<Map<String, Object>> logs =
        jdbc.queryForList(
            "select action, changed_by, changed_at from product_change_logs where product_id = ?"
                + " order by id",
            id);
    assertThat(logs).extracting(l -> l.get("action")).containsExactly("CREATED", "DELETED");
    assertThat(logs.get(1).get("changed_by")).isEqualTo("tester");
    assertThat(logs.get(1).get("changed_at")).isNotNull();
  }

  @Test
  void 이미_삭제된_상품과_없는_상품을_삭제하면_404() throws Exception {
    long id = createProduct(productBody("두 번 삭제")).get("id").asLong();
    deleteProduct(id);

    assertThat(status(deleteProduct(id))).isEqualTo(404);
    assertThat(status(deleteProduct(777777))).isEqualTo(404);
  }

  @Test
  void 삭제된_상품은_수정과_이미지_업로드도_할_수_없다() throws Exception {
    JsonNode product = createProduct(productBody("삭제 후 접근"));
    long id = product.get("id").asLong();
    String imageUrl = json(uploadImage(id, TestImages.png(100), "a.png")).get("url").asText();
    deleteProduct(id);

    Map<String, Object> body = productBody("수정 시도");
    body.put("version", product.get("version").asLong());
    MvcResult update =
        mvc.perform(
                put("/api/v1/products/{id}", id)
                    .with(manager())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
            .andReturn();

    assertThat(status(update)).isEqualTo(404);
    assertThat(status(uploadImage(id, TestImages.png(100), "b.png"))).isEqualTo(404);
    assertThat(status(mvc.perform(get(imageUrl)).andReturn())).isEqualTo(404);
  }
}
