package com.example.commerce.product.api;

/** 상품 이미지. url은 인증 없이 읽을 수 있는 이미지 파일 경로다. */
public record ProductImageResponse(Long id, String url, boolean primary, int position) {}
