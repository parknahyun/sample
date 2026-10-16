package com.example.commerce.common;

import java.util.List;

/** 입력 검증에 실패했을 때 던진다(400). 필드별 오류를 함께 담는다. */
public class ValidationFailedException extends RuntimeException {

  private final List<FieldProblem> errors;

  public ValidationFailedException(String message, List<FieldProblem> errors) {
    super(message);
    this.errors = List.copyOf(errors);
  }

  public ValidationFailedException(String field, String message) {
    this(message, List.of(new FieldProblem(field, message)));
  }

  public List<FieldProblem> getErrors() {
    return errors;
  }
}
