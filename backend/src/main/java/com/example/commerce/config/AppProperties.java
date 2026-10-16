package com.example.commerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 애플리케이션 설정. 비밀 정보는 환경 변수로만 주입한다. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Image image, Security security) {

  public record Image(String storageDir) {}

  public record Security(DevOperator devOperator) {}

  /** 개발·테스트 프로필 전용 인메모리 운영자 계정. */
  public record DevOperator(String username, String password) {

    public boolean configured() {
      return username != null && !username.isBlank() && password != null && !password.isBlank();
    }
  }

  public DevOperator devOperatorOrEmpty() {
    return security == null || security.devOperator() == null
        ? new DevOperator(null, null)
        : security.devOperator();
  }
}
