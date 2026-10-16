package com.example.commerce.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Testcontainers(PostgreSQL 16) 기반 통합 테스트 공통 클래스. 컨테이너는 모든 테스트 클래스가 하나를 공유한다. Docker가 없는 로컬 환경에서는
 * {@code ./gradlew test -PwithoutDocker}로 이 태그(integration)를 제외할 수 있다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
public abstract class IntegrationTestBase {

  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");
  // 테스트 실행마다 무작위로 만든 값이라 저장소에 비밀 정보가 남지 않는다.
  static final String OPERATOR = "operator";
  static final String OPERATOR_PASSWORD = UUID.randomUUID().toString();
  static final Path IMAGE_DIR = createImageDir();

  static {
    POSTGRES.start();
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("app.security.dev-operator.username", () -> OPERATOR);
    registry.add("app.security.dev-operator.password", () -> OPERATOR_PASSWORD);
    registry.add("app.image.storage-dir", IMAGE_DIR::toString);
  }

  @Autowired protected MockMvc mvc;
  @Autowired protected ObjectMapper objectMapper;
  @Autowired protected JdbcTemplate jdbc;

  /** 테스트마다 상품과 하위 데이터를 비운다(카테고리는 마이그레이션이 만든 값을 유지한다). */
  @BeforeEach
  void cleanDatabase() {
    jdbc.execute(
        "TRUNCATE product_change_logs, product_images, product_variants, product_option_values,"
            + " product_option_groups, products RESTART IDENTITY CASCADE");
  }

  protected static RequestPostProcessor manager() {
    return user("tester").roles("PRODUCT_MANAGER");
  }

  /** 필수 항목만 채운 상품 등록 요청 본문. */
  protected Map<String, Object> productBody(String name) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("name", name);
    body.put("price", 10000);
    body.put("categoryId", defaultCategoryId());
    body.put("stockQuantity", 5);
    return body;
  }

  protected MvcResult postProduct(Map<String, Object> body) throws Exception {
    return mvc.perform(
            post("/api/v1/products")
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andReturn();
  }

  /** 상품을 등록하고(201을 확인) 응답 본문을 돌려준다. */
  protected JsonNode createProduct(Map<String, Object> body) throws Exception {
    MvcResult result = postProduct(body);
    assertThat(result.getResponse().getStatus())
        .as("상품 등록 응답: %s", result.getResponse().getContentAsString(StandardCharsets.UTF_8))
        .isEqualTo(201);
    return json(result);
  }

  protected JsonNode json(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
  }

  protected MvcResult uploadImage(long productId, byte[] content, String filename)
      throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", filename, "application/octet-stream", content);
    return mvc.perform(
            multipart("/api/v1/products/{id}/images", productId).file(file).with(manager()))
        .andReturn();
  }

  protected long defaultCategoryId() {
    return jdbc.queryForObject("select id from categories where name = '미분류'", Long.class);
  }

  private static Path createImageDir() {
    try {
      return Files.createTempDirectory("commerce-images");
    } catch (IOException e) {
      throw new IllegalStateException("테스트용 이미지 디렉터리를 만들 수 없습니다", e);
    }
  }
}
