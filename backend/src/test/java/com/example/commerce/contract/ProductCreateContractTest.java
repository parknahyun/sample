package com.example.commerce.contract;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.example.commerce.integration.IntegrationTestBase;
import com.example.commerce.support.TestImages;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/** 계약(contracts/product-api.openapi.yaml)과 실제 응답이 일치하는지 검증한다. */
public class ProductCreateContractTest extends IntegrationTestBase {

  /** 운영자 인증은 SecurityConfig가 검증하므로 계약 검증기의 보안 헤더 검사는 끈다. */
  static final OpenApiInteractionValidator VALIDATOR =
      OpenApiInteractionValidator.createFor(
              Path.of(System.getProperty("contract.file")).toUri().toString())
          .withLevelResolver(
              LevelResolver.create()
                  .withLevel("validation.request.security.missing", ValidationReport.Level.IGNORE)
                  .build())
          .build();

  /** MockMvc의 multipart 요청은 검증기가 본문을 읽지 못하므로, 이 한 가지 검사만 끄고 응답은 그대로 검증한다. */
  static final OpenApiInteractionValidator MULTIPART_VALIDATOR =
      OpenApiInteractionValidator.createFor(
              Path.of(System.getProperty("contract.file")).toUri().toString())
          .withLevelResolver(
              LevelResolver.create()
                  .withLevel("validation.request.security.missing", ValidationReport.Level.IGNORE)
                  .withLevel("validation.request.body.missing", ValidationReport.Level.IGNORE)
                  .build())
          .build();

  /** 잘못된 요청을 일부러 보내는 테스트용: 요청 본문 스키마 위반은 무시하고 오류 응답이 계약과 일치하는지만 본다. */
  static final OpenApiInteractionValidator ERROR_RESPONSE_VALIDATOR =
      OpenApiInteractionValidator.createFor(
              Path.of(System.getProperty("contract.file")).toUri().toString())
          .withLevelResolver(
              LevelResolver.create()
                  .withLevel("validation.request.security.missing", ValidationReport.Level.IGNORE)
                  .withLevel("validation.request.body.schema", ValidationReport.Level.IGNORE)
                  .build())
          .build();

  @Test
  void 상품_등록_응답이_계약과_일치한다() throws Exception {
    mvc.perform(
            post("/api/v1/products")
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productBody("계약 상품"))))
        .andExpect(status().isCreated())
        .andExpect(openApi().isValid(VALIDATOR));
  }

  @Test
  void 옵션_상품_등록_응답이_계약과_일치한다() throws Exception {
    Map<String, Object> body = productBody("계약 옵션 상품");
    body.remove("stockQuantity");
    body.put("optionGroups", List.of(Map.of("name", "사이즈", "values", List.of("M", "L"))));
    body.put(
        "variants",
        List.of(
            Map.of("optionValues", Map.of("사이즈", "M"), "stockQuantity", 2),
            Map.of("optionValues", Map.of("사이즈", "L"), "stockQuantity", 0)));

    mvc.perform(
            post("/api/v1/products")
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isCreated())
        .andExpect(openApi().isValid(VALIDATOR));
  }

  @Test
  void 검증_실패_응답이_계약과_일치한다() throws Exception {
    mvc.perform(
            post("/api/v1/products")
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(openApi().isValid(ERROR_RESPONSE_VALIDATOR));
  }

  @Test
  void 상품_상세와_목록_응답이_계약과_일치한다() throws Exception {
    JsonNode product = createProduct(productBody("조회 상품"));

    mvc.perform(get("/api/v1/products/{id}", product.get("id").asLong()).with(manager()))
        .andExpect(status().isOk())
        .andExpect(openApi().isValid(VALIDATOR));
    mvc.perform(get("/api/v1/products").with(manager()))
        .andExpect(status().isOk())
        .andExpect(openApi().isValid(VALIDATOR));
    mvc.perform(get("/api/v1/products/{id}", 999999).with(manager()))
        .andExpect(status().isNotFound())
        .andExpect(openApi().isValid(VALIDATOR));
  }

  @Test
  void 이미지_업로드_응답이_계약과_일치한다() throws Exception {
    long id = createProduct(productBody("이미지 계약")).get("id").asLong();
    MockMultipartFile file =
        new MockMultipartFile("file", "a.png", "image/png", TestImages.png(500));

    mvc.perform(multipart("/api/v1/products/{id}/images", id).file(file).with(manager()))
        .andExpect(status().isCreated())
        .andExpect(openApi().isValid(MULTIPART_VALIDATOR));
  }

  @Test
  void 카테고리_목록_응답이_계약과_일치한다() throws Exception {
    mvc.perform(get("/api/v1/categories").with(manager()))
        .andExpect(status().isOk())
        .andExpect(openApi().isValid(VALIDATOR));
  }
}
