package com.example.commerce.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.commerce.config.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** 보안 점검 회귀 테스트: 클라이언트가 보낸 요청 ID는 안전한 형식일 때만 쓰고(로그·헤더 주입 방지), 요청이 끝나면 MDC를 비운다. */
class RequestIdFilterTest {

  private final RequestIdFilter filter = new RequestIdFilter();

  private String run(String incomingHeader) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    if (incomingHeader != null) {
      request.addHeader(RequestIdFilter.HEADER, incomingHeader);
    }
    MockHttpServletResponse response = new MockHttpServletResponse();
    String[] seenInChain = new String[1];
    MockFilterChain chain =
        new MockFilterChain() {
          @Override
          public void doFilter(
              jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
            seenInChain[0] = MDC.get(RequestIdFilter.MDC_KEY);
          }
        };
    filter.doFilter(request, response, chain);

    assertThat(MDC.get(RequestIdFilter.MDC_KEY)).as("요청이 끝나면 MDC가 비워진다").isNull();
    assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(seenInChain[0]);
    return seenInChain[0];
  }

  @Test
  void 안전한_요청_ID는_그대로_쓴다() throws Exception {
    assertThat(run("req-123.abc_DEF")).isEqualTo("req-123.abc_DEF");
  }

  @Test
  void 없으면_새로_만든다() throws Exception {
    assertThat(run(null)).matches("[0-9a-f-]{36}");
  }

  @Test
  void 줄바꿈_공백_긴_값은_버리고_새로_만든다() throws Exception {
    for (String unsafe :
        new String[] {"bad\r\nX-Injected: 1", "has space", "x".repeat(65), "<script>", ""}) {
      assertThat(run(unsafe)).as(unsafe).matches("[0-9a-f-]{36}");
    }
  }
}
