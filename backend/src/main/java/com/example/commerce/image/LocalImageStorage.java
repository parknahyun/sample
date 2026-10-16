package com.example.commerce.image;

import com.example.commerce.config.AppProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** 로컬 디렉터리에 이미지를 저장한다. 파일명은 서버가 만든 UUID이며, 모든 키는 형식을 검사하고 저장 디렉터리 밖으로 벗어나지 않는지 확인한다(경로 조작 방지). */
@Component
public class LocalImageStorage implements ImageStorage {

  /** 서버가 만든 키 형식: UUID와 확장자. */
  public static final Pattern KEY_FORMAT =
      Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg)$");

  private final Path directory;

  public LocalImageStorage(AppProperties properties) {
    String configured = properties.image() == null ? null : properties.image().storageDir();
    this.directory =
        Path.of(configured == null || configured.isBlank() ? "./data/images" : configured)
            .toAbsolutePath()
            .normalize();
  }

  @Override
  public void save(String key, byte[] content) {
    try {
      Files.createDirectories(directory);
      Files.write(resolve(key), content);
    } catch (IOException e) {
      throw new UncheckedIOException("이미지 파일을 저장하지 못했습니다", e);
    }
  }

  @Override
  public Optional<byte[]> load(String key) {
    if (!KEY_FORMAT.matcher(key).matches()) {
      return Optional.empty();
    }
    Path path = resolve(key);
    try {
      return Files.isRegularFile(path) ? Optional.of(Files.readAllBytes(path)) : Optional.empty();
    } catch (IOException e) {
      throw new UncheckedIOException("이미지 파일을 읽지 못했습니다", e);
    }
  }

  @Override
  public void delete(String key) {
    try {
      Files.deleteIfExists(resolve(key));
    } catch (IOException e) {
      throw new UncheckedIOException("이미지 파일을 삭제하지 못했습니다", e);
    }
  }

  private Path resolve(String key) {
    if (!KEY_FORMAT.matcher(key).matches()) {
      throw new IllegalArgumentException("올바르지 않은 이미지 키입니다");
    }
    Path path = directory.resolve(key).normalize();
    if (!path.startsWith(directory)) {
      throw new IllegalArgumentException("저장 디렉터리를 벗어난 경로입니다");
    }
    return path;
  }
}
