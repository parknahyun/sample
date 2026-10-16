package com.example.commerce.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.commerce.common.ValidationFailedException;
import com.example.commerce.image.ImageValidator;
import com.example.commerce.support.TestImages;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** FR-002: JPG·PNG만 파일 시그니처로 판별하고, 장당 5MB 이하만 허용한다. */
class ImageValidatorTest {

  private final ImageValidator validator = new ImageValidator();

  @Test
  void PNG_시그니처를_인식한다() {
    assertThat(validator.detectContentType(TestImages.png(100))).isEqualTo("image/png");
  }

  @Test
  void JPEG_시그니처를_인식한다() {
    assertThat(validator.detectContentType(TestImages.jpeg(100))).isEqualTo("image/jpeg");
  }

  @Test
  void GIF는_거부한다() {
    assertThatThrownBy(() -> validator.detectContentType(TestImages.gif(100)))
        .isInstanceOf(ValidationFailedException.class)
        .hasMessageContaining("JPG 또는 PNG");
  }

  @Test
  void 확장자만_이미지인_위장_파일은_거부한다() {
    assertThatThrownBy(() -> validator.detectContentType("그냥 텍스트 파일".getBytes()))
        .isInstanceOf(ValidationFailedException.class);
  }

  @Test
  void 빈_파일은_거부한다() {
    assertThatThrownBy(() -> validator.detectContentType(new byte[0]))
        .isInstanceOf(ValidationFailedException.class);
  }

  @Test
  void 정확히_5MB는_허용하고_초과하면_거부한다() {
    int limit = 5 * 1024 * 1024;
    assertThat(validator.detectContentType(TestImages.png(limit))).isEqualTo("image/png");
    assertThatThrownBy(() -> validator.detectContentType(TestImages.png(limit + 1)))
        .isInstanceOf(MaxUploadSizeExceededException.class);
  }
}
