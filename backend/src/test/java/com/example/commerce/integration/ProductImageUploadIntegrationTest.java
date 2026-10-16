package com.example.commerce.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.commerce.support.TestImages;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** 사용자 스토리 1: 이미지 업로드(quickstart 시나리오 6, 6-1). */
class ProductImageUploadIntegrationTest extends IntegrationTestBase {

  private long newProduct() throws Exception {
    return createProduct(productBody("이미지 상품")).get("id").asLong();
  }

  @Test
  void 이미지_없이_등록한_상품도_허용한다() throws Exception {
    long id = newProduct();

    MvcResult result = mvc.perform(get("/api/v1/products/{id}", id).with(manager())).andReturn();

    assertThat(json(result).get("images")).isEmpty();
  }

  @Test
  void 첫_이미지는_자동으로_대표가_되고_이후는_대표가_아니다() throws Exception {
    long id = newProduct();

    MvcResult first = uploadImage(id, TestImages.png(1000), "a.png");
    MvcResult second = uploadImage(id, TestImages.jpeg(1000), "b.jpg");

    assertThat(first.getResponse().getStatus()).isEqualTo(201);
    JsonNode firstBody = json(first);
    assertThat(firstBody.get("primary").asBoolean()).isTrue();
    assertThat(firstBody.get("url").asText()).matches("/api/v1/images/[0-9a-f-]{36}\\.png");
    assertThat(json(second).get("primary").asBoolean()).isFalse();
    assertThat(json(second).get("url").asText()).endsWith(".jpg");

    JsonNode images =
        json(mvc.perform(get("/api/v1/products/{id}", id).with(manager())).andReturn())
            .get("images");
    assertThat(images).hasSize(2);
    long primaryCount = images.findValues("primary").stream().filter(JsonNode::asBoolean).count();
    assertThat(primaryCount).isEqualTo(1);
  }

  @Test
  void 상품당_10장까지만_올릴_수_있다() throws Exception {
    long id = newProduct();
    for (int i = 0; i < 10; i++) {
      assertThat(uploadImage(id, TestImages.png(100), "i" + i + ".png").getResponse().getStatus())
          .isEqualTo(201);
    }

    MvcResult eleventh = uploadImage(id, TestImages.png(100), "i11.png");

    assertThat(eleventh.getResponse().getStatus()).isEqualTo(400);
    assertThat(json(eleventh).get("detail").asText()).contains("10장");
  }

  @Test
  void 오메가바이트_초과_파일은_413() throws Exception {
    long id = newProduct();

    MvcResult result = uploadImage(id, TestImages.png(6 * 1024 * 1024), "big.png");

    assertThat(result.getResponse().getStatus()).isEqualTo(413);
    assertThat(json(result).get("detail").asText()).contains("5MB");
  }

  @Test
  void GIF와_위장_파일은_거부한다() throws Exception {
    long id = newProduct();

    assertThat(uploadImage(id, TestImages.gif(100), "a.gif").getResponse().getStatus())
        .isEqualTo(400);
    MvcResult fake = uploadImage(id, "텍스트일 뿐".getBytes(), "fake.png");
    assertThat(fake.getResponse().getStatus()).isEqualTo(400);
    assertThat(json(fake).get("errors").toString()).contains("JPG 또는 PNG");
  }

  @Test
  void 존재하지_않는_상품에는_올릴_수_없다() throws Exception {
    assertThat(uploadImage(424242, TestImages.png(100), "a.png").getResponse().getStatus())
        .isEqualTo(404);
  }

  @Test
  void 업로드한_파일은_인증_없이_조회할_수_있다() throws Exception {
    long id = newProduct();
    byte[] content = TestImages.png(2048);
    String url = json(uploadImage(id, content, "a.png")).get("url").asText();

    MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();

    assertThat(result.getResponse().getContentType()).isEqualTo("image/png");
    assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(content);
    assertThat(result.getResponse().getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
  }

  @Test
  void 알_수_없거나_형식이_잘못된_키는_404() throws Exception {
    mvc.perform(get("/api/v1/images/00000000-0000-0000-0000-000000000000.png"))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/images/passwd")).andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/images/..passwd.png")).andExpect(status().isNotFound());
  }

  @Test
  void 업로드는_UPDATED_변경이력에_기록되고_상품_버전을_바꾸지_않는다() throws Exception {
    JsonNode product = createProduct(productBody("버전 확인"));
    long id = product.get("id").asLong();
    long versionBefore = product.get("version").asLong();

    uploadImage(id, TestImages.png(100), "a.png");

    List<Map<String, Object>> logs =
        jdbc.queryForList(
            "select action, summary::text as summary from product_change_logs where product_id = ?"
                + " order by id",
            id);
    assertThat(logs).extracting(l -> l.get("action")).containsExactly("CREATED", "UPDATED");
    assertThat((String) logs.get(1).get("summary")).contains("image");
    long versionAfter =
        json(mvc.perform(get("/api/v1/products/{id}", id).with(manager())).andReturn())
            .get("version")
            .asLong();
    assertThat(versionAfter).isEqualTo(versionBefore);
  }
}
