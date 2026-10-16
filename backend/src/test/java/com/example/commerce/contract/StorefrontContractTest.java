package com.example.commerce.contract;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.commerce.integration.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 고객 노출용 조회 API는 인증 없이 호출할 수 있고, 응답에 재고 수량·코드·version이 없다(FR-018). */
public class StorefrontContractTest extends IntegrationTestBase {

  @Test
  void 고객_목록_응답이_계약과_일치한다() throws Exception {
    createProduct(productBody("노출 상품"));

    mvc.perform(get("/api/v1/storefront/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].stockQuantity").doesNotExist())
        .andExpect(jsonPath("$.content[0].code").doesNotExist())
        .andExpect(jsonPath("$.content[0].version").doesNotExist())
        .andExpect(openApi().isValid(ProductCreateContractTest.VALIDATOR));
  }

  @Test
  void 고객_상세_응답이_계약과_일치한다() throws Exception {
    Map<String, Object> body = productBody("옵션 노출");
    body.remove("stockQuantity");
    body.put("optionGroups", List.of(Map.of("name", "사이즈", "values", List.of("M", "L"))));
    body.put(
        "variants",
        List.of(
            Map.of("optionValues", Map.of("사이즈", "M"), "stockQuantity", 3),
            Map.of("optionValues", Map.of("사이즈", "L"), "stockQuantity", 0)));
    JsonNode product = createProduct(body);

    mvc.perform(get("/api/v1/storefront/products/{id}", product.get("id").asLong()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.variants[0].stockQuantity").doesNotExist())
        .andExpect(openApi().isValid(ProductCreateContractTest.VALIDATOR));
  }

  @Test
  void 없는_상품은_404이며_계약과_일치한다() throws Exception {
    mvc.perform(get("/api/v1/storefront/products/{id}", 999999))
        .andExpect(status().isNotFound())
        .andExpect(openApi().isValid(ProductCreateContractTest.VALIDATOR));
  }
}
