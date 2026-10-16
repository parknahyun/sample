package com.example.commerce.support;

import java.util.Arrays;

/** 테스트용 이미지 바이트. 파일 시그니처(매직 넘버)만 갖춘 최소한의 데이터다. */
public final class TestImages {

  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
  private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
  private static final byte[] GIF = {'G', 'I', 'F', '8', '9', 'a'};

  private TestImages() {}

  public static byte[] png(int size) {
    return padded(PNG, size);
  }

  public static byte[] jpeg(int size) {
    return padded(JPEG, size);
  }

  public static byte[] gif(int size) {
    return padded(GIF, size);
  }

  private static byte[] padded(byte[] header, int size) {
    byte[] bytes = new byte[Math.max(size, header.length)];
    Arrays.fill(bytes, (byte) 0x01);
    System.arraycopy(header, 0, bytes, 0, header.length);
    return bytes;
  }
}
