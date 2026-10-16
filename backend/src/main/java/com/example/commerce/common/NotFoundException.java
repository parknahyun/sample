package com.example.commerce.common;

/** 존재하지 않거나 삭제된 대상에 접근했을 때 던진다(404). */
public class NotFoundException extends RuntimeException {

  public NotFoundException(String message) {
    super(message);
  }
}
