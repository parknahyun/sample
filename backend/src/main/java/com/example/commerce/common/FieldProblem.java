package com.example.commerce.common;

/** 검증에 실패한 항목과 안내 메시지. */
public record FieldProblem(String field, String message) {}
