package com.example.commerce.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** 사용자 스토리 3: 검색·필터·정렬·페이지(FR-010, FR-011). */
class ProductSearchIntegrationTest extends IntegrationTestBase {

  private MockHttpServletRequestBuilder search(String... params) {
    MockHttpServletRequestBuilder request = get("/api/v1/products").with(manager());
    for (int i = 0; i < params.length; i += 2) {
      request.param(params[i], params[i + 1]);
    }
    return request;
  }

  private JsonNode run(String... params) throws Exception {
    MvcResult result = mvc.perform(search(params)).andReturn();
    assertThat(result.getResponse().getStatus())
        .as(result.getResponse().getContentAsString())
        .isEqualTo(200);
    return json(result);
  }

  private long product(String name, long price, String status, int stock, Long categoryId)
      throws Exception {
    Map<String, Object> body = productBody(name);
    body.put("price", price);
    body.put("status", status);
    body.put("stockQuantity", stock);
    if (categoryId != null) {
      body.put("categoryId", categoryId);
    }
    return createProduct(body).get("id").asLong();
  }

  private long newCategory(String name) {
    return jdbc.queryForObject(
        "insert into categories (name) values (?) returning id", Long.class, name);
  }

  @Test
  void 상품명_일부로_검색하고_대소문자를_구분하지_않는다() throws Exception {
    product("면 티셔츠", 10000, "ON_SALE", 5, null);
    product("Cotton Shirt", 20000, "ON_SALE", 5, null);
    product("청바지", 30000, "ON_SALE", 5, null);

    assertThat(run("q", "티셔츠").get("content").findValuesAsText("name")).containsExactly("면 티셔츠");
    assertThat(run("q", "shirt").get("content").findValuesAsText("name"))
        .containsExactly("Cotton Shirt");
    assertThat(run("q", "SHIRT").get("content")).hasSize(1);
  }

  @Test
  void 검색어의_와일드카드_문자는_그대로_찾는다() throws Exception {
    product("100% 면", 10000, "ON_SALE", 5, null);
    product("일반 상품", 10000, "ON_SALE", 5, null);
    product("snake_case", 10000, "ON_SALE", 5, null);

    assertThat(run("q", "%").get("content").findValuesAsText("name")).containsExactly("100% 면");
    assertThat(run("q", "_").get("content").findValuesAsText("name")).containsExactly("snake_case");
  }

  @Test
  void 카테고리와_판매_상태로_거르고_조합할_수_있다() throws Exception {
    long shoes = newCategory("신발");
    product("운동화", 50000, "ON_SALE", 5, shoes);
    product("품절 운동화", 50000, "ON_SALE", 0, shoes);
    product("숨김 운동화", 50000, "HIDDEN", 5, shoes);
    product("티셔츠", 10000, "ON_SALE", 5, null);

    assertThat(run("categoryId", String.valueOf(shoes)).get("content")).hasSize(3);
    assertThat(run("status", "SOLD_OUT").get("content").findValuesAsText("name"))
        .containsExactly("품절 운동화");
    assertThat(
            run("categoryId", String.valueOf(shoes), "status", "HIDDEN")
                .get("content")
                .findValuesAsText("name"))
        .containsExactly("숨김 운동화");
    assertThat(run("q", "운동화", "status", "ON_SALE").get("content")).hasSize(1);
  }

  @Test
  void 등록일과_가격으로_정렬한다() throws Exception {
    product("가", 30000, "ON_SALE", 5, null);
    product("나", 10000, "ON_SALE", 5, null);
    product("다", 20000, "ON_SALE", 5, null);

    assertThat(run("sort", "price,asc").get("content").findValuesAsText("name"))
        .containsExactly("나", "다", "가");
    assertThat(run("sort", "price,desc").get("content").findValuesAsText("name"))
        .containsExactly("가", "다", "나");
    assertThat(run("sort", "createdAt,asc").get("content").findValuesAsText("name"))
        .containsExactly("가", "나", "다");
    // 기본 정렬은 등록일 내림차순이다.
    assertThat(run().get("content").findValuesAsText("name")).containsExactly("다", "나", "가");
  }

  @Test
  void 페이지로_나누고_범위를_벗어난_페이지는_빈_목록이다() throws Exception {
    for (int i = 1; i <= 5; i++) {
      product("상품" + i, i * 1000L, "ON_SALE", 5, null);
    }

    JsonNode first = run("size", "2", "page", "0", "sort", "price,asc");
    assertThat(first.get("content").findValuesAsText("name")).containsExactly("상품1", "상품2");
    assertThat(first.get("totalElements").asLong()).isEqualTo(5);
    assertThat(first.get("totalPages").asInt()).isEqualTo(3);
    assertThat(run("size", "2", "page", "2", "sort", "price,asc").get("content")).hasSize(1);

    JsonNode beyond = run("size", "2", "page", "10");
    assertThat(beyond.get("content")).isEmpty();
    assertThat(beyond.get("totalElements").asLong()).isEqualTo(5);
  }

  @Test
  void 조건에_맞는_상품이_없으면_빈_목록을_돌려준다() throws Exception {
    product("티셔츠", 10000, "ON_SALE", 5, null);

    JsonNode result = run("q", "존재하지않는검색어");

    assertThat(result.get("content")).isEmpty();
    assertThat(result.get("totalElements").asLong()).isZero();
    assertThat(result.get("totalPages").asInt()).isZero();
  }

  @Test
  void 삭제된_상품은_검색되지_않는다() throws Exception {
    long id = product("삭제될 상품", 10000, "ON_SALE", 5, null);
    jdbc.update("update products set deleted_at = now() where id = ?", id);

    assertThat(run("q", "삭제될").get("content")).isEmpty();
  }

  @Test
  void 페이지_크기는_1에서_100까지만_허용한다() throws Exception {
    assertThat(run("size", "100").get("size").asInt()).isEqualTo(100);

    assertThat(mvc.perform(search("size", "101")).andReturn().getResponse().getStatus())
        .isEqualTo(400);
    assertThat(mvc.perform(search("size", "0")).andReturn().getResponse().getStatus())
        .isEqualTo(400);
    assertThat(mvc.perform(search("page", "-1")).andReturn().getResponse().getStatus())
        .isEqualTo(400);
  }

  @Test
  void 허용되지_않는_정렬과_상태는_400() throws Exception {
    MvcResult badSort = mvc.perform(search("sort", "name,asc")).andReturn();
    assertThat(badSort.getResponse().getStatus()).isEqualTo(400);
    assertThat(json(badSort).get("errors").toString()).contains("sort");

    assertThat(mvc.perform(search("status", "UNKNOWN")).andReturn().getResponse().getStatus())
        .isEqualTo(400);
  }
}
