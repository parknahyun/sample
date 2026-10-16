package com.example.commerce.image;

import com.example.commerce.common.ValidationFailedException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 이미지 업로드 검증(FR-002). 확장자나 클라이언트가 보낸 content type이 아니라 파일 시그니처(매직 넘버)로 JPG·PNG를 판별하고, 장당 5MB 이하만
 * 허용한다.
 */
@Component
public class ImageValidator {

  public static final int MAX_BYTES = 5 * 1024 * 1024;
  public static final String PNG = "image/png";
  public static final String JPEG = "image/jpeg";

  private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
  private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

  /**
   * 파일 내용을 검사해 content type을 돌려준다.
   *
   * @throws MaxUploadSizeExceededException 5MB를 초과할 때(413)
   * @throws ValidationFailedException 비어 있거나 JPG·PNG가 아닐 때(400)
   */
  public String detectContentType(byte[] content) {
    if (content.length == 0) {
      throw new ValidationFailedException("file", "비어 있는 파일은 올릴 수 없습니다.");
    }
    if (content.length > MAX_BYTES) {
      throw new MaxUploadSizeExceededException(MAX_BYTES);
    }
    if (startsWith(content, PNG_SIGNATURE)) {
      return PNG;
    }
    if (startsWith(content, JPEG_SIGNATURE)) {
      return JPEG;
    }
    throw new ValidationFailedException("file", "JPG 또는 PNG 형식의 이미지만 올릴 수 있습니다.");
  }

  /** content type에 맞는 저장 파일 확장자. */
  public static String extensionOf(String contentType) {
    return PNG.equals(contentType) ? "png" : "jpg";
  }

  private static boolean startsWith(byte[] content, byte[] signature) {
    if (content.length < signature.length) {
      return false;
    }
    for (int i = 0; i < signature.length; i++) {
      if (content[i] != signature[i]) {
        return false;
      }
    }
    return true;
  }
}
