package com.example.commerce.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.commerce.config.AppProperties;
import com.example.commerce.image.LocalImageStorage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 보안 점검 회귀 테스트: 이미지 저장소는 서버가 만든 키 형식만 받고 저장 디렉터리를 벗어나지 않는다. */
class LocalImageStorageTest {

  @TempDir Path directory;

  private LocalImageStorage storage;

  @BeforeEach
  void setUp() {
    storage =
        new LocalImageStorage(
            new AppProperties(new AppProperties.Image(directory.toString()), null));
  }

  @Test
  void 서버가_만든_키로_저장하고_다시_읽는다() {
    String key = UUID.randomUUID() + ".png";

    storage.save(key, new byte[] {1, 2, 3});

    assertThat(storage.load(key)).contains(new byte[] {1, 2, 3});
    assertThat(Files.exists(directory.resolve(key))).isTrue();
  }

  @Test
  void 저장_디렉터리를_벗어나려는_키는_저장할_수_없다() {
    for (String key :
        new String[] {
          "../escape.png", "..\\escape.png", "/etc/passwd", "a/b.png", "x.png", "..%2Fescape.png"
        }) {
      assertThatThrownBy(() -> storage.save(key, new byte[] {1}))
          .as(key)
          .isInstanceOf(IllegalArgumentException.class);
    }
    assertThat(directory.getParent().resolve("escape.png")).doesNotExist();
  }

  @Test
  void 형식이_틀린_키는_조회해도_비어_있고_삭제할_수_없다() {
    assertThat(storage.load("../secret.png")).isEmpty();
    assertThat(storage.load("passwd")).isEmpty();
    assertThatThrownBy(() -> storage.delete("../secret.png"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 없는_파일은_비어_있다() {
    assertThat(storage.load(UUID.randomUUID() + ".jpg")).isEmpty();
  }
}
