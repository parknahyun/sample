package com.example.commerce.common;

/** 중복 코드나 동시 수정처럼 현재 상태와 충돌하는 요청일 때 던진다(409). */
public class ConflictException extends RuntimeException {

  public ConflictException(String message) {
    super(message);
  }
}
