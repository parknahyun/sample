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
import org.springframework.test.web.servlet.MvcResult;

/** 사용자 스토리 2: 이미지 삭제와 대표 이미지 지정(FR-002). */
class ProductImageManageIntegrationTest extends IntegrationTestBase {

  private long productId;

  private JsonNode images() throws Exception {
    return json(mvc.perform(get("/api/v1/products/{id}", productId).with(manager())).andReturn())
        .get("images");
  }

  private long upload(byte[] bytes, String name) throws Exception {
    return json(uploadImage(productId, bytes, name)).get("id").asLong();
  }

  private MvcResult deleteImage(long imageId) throws Exception {
    return mvc.perform(
            delete("/api/v1/products/{p}/images/{i}", productId, imageId).with(manager()))
        .andReturn();
  }

  private long primaryCount() throws Exception {
    return images().findValues("primary").stream().filter(JsonNode::asBoolean).count();
  }

  private void newProduct() throws Exception {
    productId = createProduct(productBody("이미지 관리")).get("id").asLong();
  }

  @Test
  void 대표_이미지를_바꾸면_대표가_정확히_하나로_유지된다() throws Exception {
    newProduct();
    long first = upload(TestImages.png(100), "a.png");
    long second = upload(TestImages.png(100), "b.png");
    long third = upload(TestImages.png(100), "c.png");

    MvcResult result =
        mvc.perform(
                put("/api/v1/products/{p}/images/{i}/primary", productId, third).with(manager()))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(204);
    assertThat(primaryCount()).isEqualTo(1);
    for (JsonNode image : images()) {
      assertThat(image.get("primary").asBoolean()).isEqualTo(image.get("id").asLong() == third);
    }
    assertThat(List.of(first, second)).doesNotContain(third);
  }

  @Test
  void 대표_이미지를_삭제하면_남은_첫_이미지가_대표가_된다() throws Exception {
    newProduct();
    long first = upload(TestImages.png(100), "a.png");
    long second = upload(TestImages.png(100), "b.png");
    upload(TestImages.png(100), "c.png");

    assertThat(deleteImage(first).getResponse().getStatus()).isEqualTo(204);

    JsonNode remaining = images();
    assertThat(remaining).hasSize(2);
    assertThat(primaryCount()).isEqualTo(1);
    assertThat(remaining.get(0).get("id").asLong()).isEqualTo(second);
    assertThat(remaining.get(0).get("primary").asBoolean()).isTrue();
  }

  @Test
  void 마지막_한_장도_삭제할_수_있다() throws Exception {
    newProduct();
    long only = upload(TestImages.png(100), "a.png");

    assertThat(deleteImage(only).getResponse().getStatus()).isEqualTo(204);

    assertThat(images()).isEmpty();
  }

  @Test
  void 대표가_아닌_이미지를_삭제해도_대표는_유지된다() throws Exception {
    newProduct();
    long first = upload(TestImages.png(100), "a.png");
    long second = upload(TestImages.png(100), "b.png");

    deleteImage(second);

    JsonNode remaining = images();
    assertThat(remaining).hasSize(1);
    assertThat(remaining.get(0).get("id").asLong()).isEqualTo(first);
    assertThat(remaining.get(0).get("primary").asBoolean()).isTrue();
  }

  @Test
  void 삭제한_이미지_파일은_더_이상_조회되지_않는다() throws Exception {
    newProduct();
    JsonNode uploaded = json(uploadImage(productId, TestImages.png(100), "a.png"));
    String url = uploaded.get("url").asText();
    assertThat(mvc.perform(get(url)).andReturn().getResponse().getStatus()).isEqualTo(200);

    deleteImage(uploaded.get("id").asLong());

    assertThat(mvc.perform(get(url)).andReturn().getResponse().getStatus()).isEqualTo(404);
  }

  @Test
  void 다른_상품의_이미지나_없는_이미지는_404() throws Exception {
    newProduct();
    long mine = upload(TestImages.png(100), "a.png");
    long otherProduct = createProduct(productBody("다른 상품")).get("id").asLong();

    assertThat(
            mvc.perform(
                    delete("/api/v1/products/{p}/images/{i}", otherProduct, mine).with(manager()))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(404);
    assertThat(deleteImage(999999).getResponse().getStatus()).isEqualTo(404);
    assertThat(
            mvc.perform(
                    put("/api/v1/products/{p}/images/{i}/primary", productId, 999999)
                        .with(manager()))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(404);
  }

  @Test
  void 이미지_삭제와_대표_변경은_UPDATED_이력에_기록되고_상품_버전을_바꾸지_않는다() throws Exception {
    newProduct();
    long first = upload(TestImages.png(100), "a.png");
    long second = upload(TestImages.png(100), "b.png");
    long versionBefore =
        json(mvc.perform(get("/api/v1/products/{id}", productId).with(manager())).andReturn())
            .get("version")
            .asLong();

    mvc.perform(put("/api/v1/products/{p}/images/{i}/primary", productId, second).with(manager()));
    deleteImage(first);

    List<Map<String, Object>> logs =
        jdbc.queryForList(
            "select action, summary::text as summary from product_change_logs where product_id = ?"
                + " order by id",
            productId);
    // CREATED, 업로드 2건, 대표 변경, 삭제
    assertThat(logs).extracting(l -> l.get("action")).startsWith("CREATED", "UPDATED", "UPDATED");
    assertThat(logs).hasSize(5);
    assertThat((String) logs.get(3).get("summary")).contains("primary");
    assertThat((String) logs.get(4).get("summary")).contains("deleted");
    long versionAfter =
        json(mvc.perform(get("/api/v1/products/{id}", productId).with(manager())).andReturn())
            .get("version")
            .asLong();
    assertThat(versionAfter).isEqualTo(versionBefore);
  }
}
