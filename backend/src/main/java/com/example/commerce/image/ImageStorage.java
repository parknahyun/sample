package com.example.commerce.image;

import java.util.Optional;

/** 이미지 파일 저장소. 현재는 로컬 파일 시스템만 구현하고, 인터페이스 뒤에 두어 테스트 격리와 이후 교체를 쉽게 한다. */
public interface ImageStorage {

  void save(String key, byte[] content);

  Optional<byte[]> load(String key);

  void delete(String key);
}
