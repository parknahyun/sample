package com.example.commerce.contract;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.commerce.integration.IntegrationTestBase;
import org.junit.jupiter.api.Test;

/** 삭제 API의 204·404 응답이 계약과 일치하는지 검증한다. */
public class ProductDeleteContractTest extends IntegrationTestBase {

  @Test
  void 삭제는_204이며_계약과_일치한다() throws Exception {
    long id = createProduct(productBody("계약 삭제")).get("id").asLong();

    mvc.perform(delete("/api/v1/products/{id}", id).with(manager()))
        .andExpect(status().isNoContent())
        .andExpect(openApi().isValid(ProductCreateContractTest.VALIDATOR));
  }

  @Test
  void 없는_상품_삭제는_404이며_계약과_일치한다() throws Exception {
    mvc.perform(delete("/api/v1/products/{id}", 999999).with(manager()))
        .andExpect(status().isNotFound())
        .andExpect(openApi().isValid(ProductCreateContractTest.VALIDATOR));
  }
}
