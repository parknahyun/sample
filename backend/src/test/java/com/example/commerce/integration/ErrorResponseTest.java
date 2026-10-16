package com.example.commerce.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.commerce.config.SecurityConfig;
import com.example.commerce.support.ProbeController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** 오류 응답은 Problem Details(RFC 9457) 형식이며 requestId와 조치 가능한 한글 메시지를 담는다. */
@WebMvcTest(controllers = ProbeController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class ErrorResponseTest {

  private static final RequestPostProcessor MANAGER = user("manager").roles("PRODUCT_MANAGER");

  @Autowired MockMvc mvc;

  @Test
  void 검증_실패는_400과_필드별_오류를_반환한다() throws Exception {
    mvc.perform(
            post("/api/v1/probe/validate")
                .with(MANAGER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"count\":0}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.requestId").value(notNullValue()))
        .andExpect(jsonPath("$.errors[?(@.field=='name')].message").value("이름은 필수입니다"))
        .andExpect(jsonPath("$.errors[?(@.field=='count')].message").value("수량은 1 이상이어야 합니다"));
  }

  @Test
  void 요청에_넣은_요청_ID가_응답에_그대로_돌아온다() throws Exception {
    mvc.perform(get("/api/v1/probe/not-found").with(MANAGER).header("X-Request-Id", "req-12345"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.requestId").value("req-12345"))
        .andExpect(jsonPath("$.detail").value("존재하지 않는 상품입니다."));
  }

  @Test
  void 충돌은_409와_한글_안내를_반환한다() throws Exception {
    mvc.perform(get("/api/v1/probe/conflict").with(MANAGER))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.detail").value("이미 사용 중인 코드입니다. 다른 코드를 입력하세요."));
  }

  @Test
  void 낙관적_락_충돌은_최신_내용을_다시_불러오라고_안내한다() throws Exception {
    mvc.perform(get("/api/v1/probe/optimistic").with(MANAGER))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value(containsString("최신 내용을 다시 불러")));
  }

  @Test
  void 형식이_잘못된_JSON은_400() throws Exception {
    mvc.perform(
            post("/api/v1/probe/validate")
                .with(MANAGER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ not json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value(containsString("요청 형식")));
  }

  @Test
  void 업로드_용량_초과는_413() throws Exception {
    mvc.perform(get("/api/v1/probe/too-large").with(MANAGER))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.detail").value(containsString("5MB")));
  }

  @Test
  void 예상하지_못한_오류는_500이며_내부_정보를_노출하지_않는다() throws Exception {
    mvc.perform(get("/api/v1/probe/boom").with(MANAGER))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.requestId").value(notNullValue()))
        .andExpect(jsonPath("$.detail").value(not(containsString("내부 구현 세부 정보"))));
  }
}
