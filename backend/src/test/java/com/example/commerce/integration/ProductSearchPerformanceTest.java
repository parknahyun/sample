package com.example.commerce.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** SC-002: 상품 1만 개 중에서 검색해 찾는 데 5초 이내(내부 목표: p95 1초 미만). */
class ProductSearchPerformanceTest extends IntegrationTestBase {

  private static final int PRODUCT_COUNT = 10_000;
  private static final int SAMPLES = 60;

  @Test
  void 상품_1만_개에서_검색_응답은_p95_1초_미만이다() throws Exception {
    long categoryId = defaultCategoryId();
    jdbc.update(
        "insert into products (code, name, price, stock_quantity, status, category_id, version,"
            + " created_at, updated_at) select 'PRD-' || lpad(g::text, 6, '0'),"
            + " '상품 ' || g || case when g % 7 = 0 then ' 면 티셔츠' else ' 일반 용품' end,"
            + " 1000 + g, 5, 'ON_SALE', ?, 0, now() - (g || ' minutes')::interval, now()"
            + " from generate_series(1, ?) g",
        categoryId, PRODUCT_COUNT);
    jdbc.execute("ANALYZE products");

    // 워밍업(JIT·커넥션·쿼리 계획 캐시)
    mvc.perform(get("/api/v1/products").with(manager()).param("q", "티셔츠")).andReturn();

    List<Long> millis = new ArrayList<>();
    for (int i = 0; i < SAMPLES; i++) {
      String query =
          switch (i % 4) {
            case 0 -> "티셔츠";
            case 1 -> "상품 " + (100 + i);
            case 2 -> "일반";
            default -> "존재하지 않는 상품";
          };
      long start = System.nanoTime();
      var result =
          mvc.perform(
                  get("/api/v1/products")
                      .with(manager())
                      .param("q", query)
                      .param("sort", i % 2 == 0 ? "price,asc" : "createdAt,desc"))
              .andReturn();
      millis.add((System.nanoTime() - start) / 1_000_000);
      assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    Collections.sort(millis);
    long p95 = millis.get((int) Math.ceil(SAMPLES * 0.95) - 1);
    assertThat(p95).as("p95 응답 시간(ms), 전체: %s", millis).isLessThan(1000);
  }
}
