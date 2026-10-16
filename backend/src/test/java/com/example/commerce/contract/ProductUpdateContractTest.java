package com.example.commerce.contract;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.commerce.integration.IntegrationTestBase;
import com.example.commerce.support.TestImages;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** 수정·이미지 관리 API 응답이 계약과 일치하는지 검증한다. */
public class ProductUpdateContractTest extends IntegrationTestBase {

  @Test
  void 상품_수정_응답이_계약과_일치한다() throws Exception {
    JsonNode product = createProduct(productBody("계약 수정"));
    Map<String, Object> body = new LinkedHashMap<>(productBody("계약 수정 완료"));
    body.put("version", product.get("version").asLong());

    mvc.perform(
            put("/api/v1/products/{id}", product.get("id").asLong())
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isOk())
        .andExpect(openApi().isValid(ProductCreateContractTest.VALIDATOR));
  }

  @Test
  void 버전_충돌_응답이_계약과_일치한다() throws Exception {
    JsonNode product = createProduct(productBody("계약 충돌"));
    Map<String, Object> body = new LinkedHashMap<>(productBody("충돌"));
    body.put("version", product.get("version").asLong() + 5);

    mvc.perform(
            put("/api/v1/products/{id}", product.get("id").asLong())
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isConflict())
        .andExpect(openApi().isValid(ProductCreateContractTest.VALIDATOR));
  }

  @Test
  void 이미지_삭제와_대표_지정_응답이_계약과_일치한다() throws Exception {
    long productId = createProduct(productBody("계약 이미지")).get("id").asLong();
    long first = json(uploadImage(productId, TestImages.png(100), "a.png")).get("id").asLong();
    long second = json(uploadImage(productId, TestImages.png(100), "b.png")).get("id").asLong();

    mvc.perform(put("/api/v1/products/{p}/images/{i}/primary", productId, second).with(manager()))
        .andExpect(status().isNoContent())
        .andExpect(openApi().isValid(ProductCreateContractTest.VALIDATOR));
    mvc.perform(delete("/api/v1/products/{p}/images/{i}", productId, first).with(manager()))
        .andExpect(status().isNoContent())
        .andExpect(openApi().isValid(ProductCreateContractTest.VALIDATOR));
    mvc.perform(delete("/api/v1/products/{p}/images/{i}", productId, 999999).with(manager()))
        .andExpect(status().isNotFound())
        .andExpect(openApi().isValid(ProductCreateContractTest.VALIDATOR));
  }
}
