package com.example.commerce.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * 보안 설정. 상품 관리 API는 PRODUCT_MANAGER 역할만 허용하고, 고객 노출용 조회와 이미지 파일은 읽기(GET)만 인증 없이 허용한다. 운영 환경의 인증 방식은
 * 별도 기능에서 정하므로, 개발·테스트 프로필이 아니면 인증 가능한 계정이 없다(기본 거부).
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AppProperties.class)
public class SecurityConfig {

  public static final String ROLE_PRODUCT_MANAGER = "PRODUCT_MANAGER";
  private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
    AuthenticationEntryPoint unauthorized =
        (request, response, ex) ->
            writeProblem(
                mapper,
                response,
                HttpStatus.UNAUTHORIZED,
                "인증이 필요합니다",
                "인증 정보가 없거나 올바르지 않습니다. 로그인한 뒤 다시 시도하세요.");
    AccessDeniedHandler forbidden =
        (request, response, ex) ->
            writeProblem(
                mapper,
                response,
                HttpStatus.FORBIDDEN,
                "권한이 없습니다",
                "상품 관리 권한(PRODUCT_MANAGER)이 있는 운영자만 사용할 수 있습니다.");

    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // WWW-Authenticate 헤더 없이 JSON으로만 응답해 브라우저의 인증 대화상자가 뜨지 않게 한다.
        .httpBasic(basic -> basic.authenticationEntryPoint(unauthorized))
        .exceptionHandling(
            e -> e.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.GET, "/api/v1/storefront/**", "/api/v1/images/**")
                    .permitAll()
                    .requestMatchers("/api/v1/**")
                    .hasRole(ROLE_PRODUCT_MANAGER)
                    .anyRequest()
                    .denyAll());
    return http.build();
  }

  /** 개발·테스트 프로필: 환경 변수로 주입한 계정 하나를 PRODUCT_MANAGER로 등록한다. */
  @Bean
  @Profile({"dev", "test"})
  UserDetailsService developmentUsers(AppProperties properties) {
    AppProperties.DevOperator operator = properties.devOperatorOrEmpty();
    if (!operator.configured()) {
      log.warn("개발용 운영자 계정이 설정되지 않았습니다(DEV_OPERATOR_USERNAME, DEV_OPERATOR_PASSWORD).");
      return new InMemoryUserDetailsManager();
    }
    var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    return new InMemoryUserDetailsManager(
        User.withUsername(operator.username())
            .password(encoder.encode(operator.password()))
            .roles(ROLE_PRODUCT_MANAGER)
            .build());
  }

  /** 그 밖의 프로필: 스프링 부트가 임시 비밀번호 계정을 만들지 않도록 빈 사용자 저장소를 둔다. */
  @Bean
  @Profile("!dev & !test")
  UserDetailsService noUsers() {
    return new InMemoryUserDetailsManager();
  }

  private static void writeProblem(
      ObjectMapper mapper,
      HttpServletResponse response,
      HttpStatus status,
      String title,
      String detail)
      throws IOException {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "about:blank");
    body.put("title", title);
    body.put("status", status.value());
    body.put("detail", detail);
    body.put("requestId", MDC.get(RequestIdFilter.MDC_KEY));
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    mapper.writeValue(response.getWriter(), body);
  }
}
