package com.example.commerce.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.commerce.config.SecurityConfig;
import com.example.commerce.support.ProbeController;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** FR-014: 상품 관리 API는 PRODUCT_MANAGER 역할에만 허용하고, 고객 조회·이미지 파일은 인증 없이 읽을 수 있다. */
@WebMvcTest(controllers = ProbeController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class SecurityIntegrationTest {

  private static final String OPERATOR = "operator";
  // 테스트 실행마다 무작위로 만든 값이라 저장소에 비밀 정보가 남지 않는다.
  private static final String OPERATOR_PASSWORD = UUID.randomUUID().toString();

  @DynamicPropertySource
  static void operatorProperties(DynamicPropertyRegistry registry) {
    registry.add("app.security.dev-operator.username", () -> OPERATOR);
    registry.add("app.security.dev-operator.password", () -> OPERATOR_PASSWORD);
  }

  @Autowired MockMvc mvc;

  @Test
  void 인증_없이_운영자_API를_호출하면_401() throws Exception {
    mvc.perform(get("/api/v1/probe"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
  }

  @Test
  void 다른_역할로_운영자_API를_호출하면_403() throws Exception {
    mvc.perform(get("/api/v1/probe").with(user("viewer").roles("VIEWER")))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
  }

  @Test
  void PRODUCT_MANAGER_역할은_접근할_수_있다() throws Exception {
    mvc.perform(get("/api/v1/probe").with(user("manager").roles("PRODUCT_MANAGER")))
        .andExpect(status().isOk());
  }

  @Test
  void 개발_프로필의_HTTP_Basic_운영자_계정으로_인증할_수_있다() throws Exception {
    mvc.perform(get("/api/v1/probe").with(httpBasic(OPERATOR, OPERATOR_PASSWORD)))
        .andExpect(status().isOk());
  }

  @Test
  void 잘못된_비밀번호는_401() throws Exception {
    mvc.perform(get("/api/v1/probe").with(httpBasic(OPERATOR, "wrong-password")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void 고객_조회_API는_인증_없이_읽을_수_있다() throws Exception {
    mvc.perform(get("/api/v1/storefront/probe")).andExpect(status().isOk());
  }

  @Test
  void 이미지_파일은_인증_없이_읽을_수_있다() throws Exception {
    mvc.perform(get("/api/v1/images/probe")).andExpect(status().isOk());
  }

  @Test
  void 고객_조회_경로라도_읽기_이외의_요청은_보호된다() throws Exception {
    mvc.perform(
            post("/api/v1/storefront/probe")
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isUnauthorized());
  }
}
