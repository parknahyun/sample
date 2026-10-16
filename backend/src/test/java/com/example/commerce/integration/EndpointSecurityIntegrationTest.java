package com.example.commerce.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.example.commerce.support.TestImages;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 보안 점검 회귀 테스트(FR-014): 실제 컨트롤러가 등록된 상태에서 모든 운영자 엔드포인트가 인증과 PRODUCT_MANAGER 역할을 요구하고, 공개 경로는
 * 읽기(GET)만 인증 없이 열려 있는지 확인한다.
 */
class EndpointSecurityIntegrationTest extends IntegrationTestBase {

  /** 운영자 전용 엔드포인트 전체(메서드와 경로). 새 엔드포인트를 만들면 여기에 추가해야 한다. */
  private List<MockHttpServletRequestBuilder> managerEndpoints() {
    MockMultipartFile file =
        new MockMultipartFile("file", "a.png", "image/png", TestImages.png(10));
    return List.of(
        get("/api/v1/products"),
        get("/api/v1/products/1"),
        post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("{}"),
        put("/api/v1/products/1").contentType(MediaType.APPLICATION_JSON).content("{}"),
        delete("/api/v1/products/1"),
        multipart("/api/v1/products/1/images").file(file),
        delete("/api/v1/products/1/images/1"),
        put("/api/v1/products/1/images/1/primary"),
        get("/api/v1/categories"));
  }

  private int statusOf(MockHttpServletRequestBuilder request) throws Exception {
    return mvc.perform(request).andReturn().getResponse().getStatus();
  }

  @Test
  void 운영자_엔드포인트는_인증_없이_호출하면_모두_401() throws Exception {
    for (MockHttpServletRequestBuilder request : managerEndpoints()) {
      assertThat(statusOf(request))
          .as(
              "%s",
              request
                  .buildRequest(new org.springframework.mock.web.MockServletContext())
                  .getRequestURI())
          .isEqualTo(401);
    }
  }

  @Test
  void 운영자_엔드포인트는_다른_역할이면_모두_403() throws Exception {
    for (MockHttpServletRequestBuilder request : managerEndpoints()) {
      assertThat(statusOf(request.with(user("viewer").roles("VIEWER"))))
          .as(
              "%s",
              request
                  .buildRequest(new org.springframework.mock.web.MockServletContext())
                  .getRequestURI())
          .isEqualTo(403);
    }
  }

  @Test
  void 개발용_HTTP_Basic_계정으로_운영자_API를_호출할_수_있고_잘못된_비밀번호는_401() throws Exception {
    assertThat(statusOf(get("/api/v1/categories").with(httpBasic(OPERATOR, OPERATOR_PASSWORD))))
        .isEqualTo(200);
    assertThat(statusOf(get("/api/v1/categories").with(httpBasic(OPERATOR, "틀린-비밀번호"))))
        .isEqualTo(401);
  }

  @Test
  void 인증_실패_응답은_브라우저_인증_대화상자를_띄우지_않는다() throws Exception {
    var response = mvc.perform(get("/api/v1/products")).andReturn().getResponse();

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getHeader("WWW-Authenticate")).isNull();
  }

  @Test
  void 공개_경로도_읽기_이외의_요청은_보호된다() throws Exception {
    assertThat(statusOf(post("/api/v1/storefront/products"))).isEqualTo(401);
    assertThat(statusOf(delete("/api/v1/storefront/products/1"))).isEqualTo(401);
    assertThat(statusOf(post("/api/v1/images/x.png"))).isEqualTo(401);
    assertThat(statusOf(delete("/api/v1/images/x.png"))).isEqualTo(401);
  }

  @Test
  void 정의하지_않은_경로와_API_문서_경로는_인증_없이_접근할_수_없다() throws Exception {
    assertThat(statusOf(get("/v3/api-docs"))).isIn(401, 403);
    assertThat(statusOf(get("/actuator/env"))).isIn(401, 403);
    assertThat(statusOf(get("/swagger-ui/index.html"))).isIn(401, 403);
  }
}
