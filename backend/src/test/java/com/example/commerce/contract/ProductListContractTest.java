package com.example.commerce.contract;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.commerce.integration.IntegrationTestBase;
import org.junit.jupiter.api.Test;

/** 목록 조회의 검색·필터·정렬·페이지 파라미터와 빈 결과 응답이 계약과 일치하는지 검증한다. */
public class ProductListContractTest extends IntegrationTestBase {

  @Test
  void 모든_조회_파라미터를_함께_쓴_응답이_계약과_일치한다() throws Exception {
    createProduct(productBody("면 티셔츠"));

    mvc.perform(
            get("/api/v1/products")
                .with(manager())
                .param("q", "티셔츠")
                .param("categoryId", String.valueOf(defaultCategoryId()))
                .param("status", "ON_SALE")
                .param("sort", "price,asc")
                .param("page", "0")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("면 티셔츠"))
        .andExpect(openApi().isValid(ProductCreateContractTest.VALIDATOR));
  }

  @Test
  void 빈_결과_응답이_계약과_일치한다() throws Exception {
    mvc.perform(get("/api/v1/products").with(manager()).param("q", "없는 상품"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(openApi().isValid(ProductCreateContractTest.VALIDATOR));
  }
}
