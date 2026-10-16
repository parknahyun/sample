package com.example.commerce.product.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 옵션 값 조합을 정규화한 키(예: {@code 색상=빨강|사이즈=M}). 옵션 그룹 순서를 따르므로 입력 순서와 무관하게 같은 조합은 같은 키가 된다. 옵션 이름과 값에는
 * 구분자('=', '|')를 쓸 수 없다.
 */
public final class VariantCombinationKey {

  private static final String PAIR_SEPARATOR = "|";
  private static final String VALUE_SEPARATOR = "=";

  private VariantCombinationKey() {}

  public static boolean isValidToken(String token) {
    return token != null
        && !token.isBlank()
        && !token.contains(PAIR_SEPARATOR)
        && !token.contains(VALUE_SEPARATOR);
  }

  /**
   * 옵션 그룹 순서에 맞춘 조합 키를 만든다.
   *
   * @throws IllegalArgumentException 옵션 그룹의 값이 빠졌을 때
   */
  public static String key(List<String> groupOrder, Map<String, String> optionValues) {
    List<String> pairs = new ArrayList<>();
    for (String group : groupOrder) {
      String value = optionValues.get(group);
      if (value == null) {
        throw new IllegalArgumentException("옵션 '" + group + "'의 값이 없습니다");
      }
      pairs.add(group + VALUE_SEPARATOR + value);
    }
    return String.join(PAIR_SEPARATOR, pairs);
  }

  /** 모든 옵션 그룹에서 값을 하나씩 고른 조합 전체. */
  public static List<String> allKeys(Map<String, List<String>> groups) {
    List<String> keys = new ArrayList<>(List.of(""));
    for (Map.Entry<String, List<String>> group : groups.entrySet()) {
      List<String> next = new ArrayList<>();
      for (String prefix : keys) {
        for (String value : group.getValue()) {
          String pair = group.getKey() + VALUE_SEPARATOR + value;
          next.add(prefix.isEmpty() ? pair : prefix + PAIR_SEPARATOR + pair);
        }
      }
      keys = next;
    }
    return keys;
  }

  /** 있어야 하지만 제공되지 않은 조합. */
  public static List<String> missing(Map<String, List<String>> groups, List<String> provided) {
    Set<String> given = new LinkedHashSet<>(provided);
    return allKeys(groups).stream().filter(key -> !given.contains(key)).toList();
  }

  /** 옵션 정의에 없는 조합. */
  public static List<String> unknown(Map<String, List<String>> groups, List<String> provided) {
    Set<String> expected = new LinkedHashSet<>(allKeys(groups));
    return provided.stream().filter(key -> !expected.contains(key)).toList();
  }

  /** 키를 옵션 이름과 값으로 되돌린다(옵션 그룹 순서 유지). */
  public static Map<String, String> parse(String key) {
    Map<String, String> values = new LinkedHashMap<>();
    for (String pair : key.split("\\" + PAIR_SEPARATOR)) {
      int index = pair.indexOf(VALUE_SEPARATOR);
      values.put(pair.substring(0, index), pair.substring(index + 1));
    }
    return values;
  }
}
