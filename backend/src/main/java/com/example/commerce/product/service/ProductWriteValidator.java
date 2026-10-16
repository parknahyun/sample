package com.example.commerce.product.service;

import com.example.commerce.common.FieldProblem;
import com.example.commerce.common.ValidationFailedException;
import com.example.commerce.product.api.OptionGroupDto;
import com.example.commerce.product.api.VariantDto;
import com.example.commerce.product.domain.ProductStatus;
import com.example.commerce.product.domain.VariantCombinationKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 등록·수정 공통 검증. Bean Validation으로 잡을 수 없는 규칙(상태, 옵션 정의와 옵션 조합의 일관성)을 검사한다.
 *
 * <p>옵션이 있는 상품은 모든 옵션 값 조합의 재고를 빠짐없이, 중복 없이 보내야 한다(FR-015).
 */
final class ProductWriteValidator {

  private ProductWriteValidator() {}

  static void validate(ProductWriteCommand command) {
    List<FieldProblem> errors = new ArrayList<>();

    if (command.status() == ProductStatus.SOLD_OUT) {
      errors.add(new FieldProblem("status", "품절은 재고로 결정되므로 지정할 수 없습니다. 판매중 또는 숨김만 지정하세요."));
    }

    List<OptionGroupDto> groups = command.optionGroupsOrEmpty();
    List<VariantDto> variants = command.variantsOrEmpty();
    if (groups.isEmpty()) {
      if (!variants.isEmpty()) {
        errors.add(new FieldProblem("variants", "옵션이 없는 상품에는 옵션 조합을 지정할 수 없습니다."));
      }
    } else {
      validateOptions(groups, variants, errors);
    }

    if (!errors.isEmpty()) {
      throw new ValidationFailedException("입력값을 확인하세요.", errors);
    }
  }

  private static void validateOptions(
      List<OptionGroupDto> groups, List<VariantDto> variants, List<FieldProblem> errors) {
    Map<String, List<String>> groupMap = new LinkedHashMap<>();
    boolean definitionsValid = true;
    for (OptionGroupDto group : groups) {
      if (!VariantCombinationKey.isValidToken(group.name())) {
        errors.add(new FieldProblem("optionGroups", invalidTokenMessage("옵션 이름", group.name())));
        definitionsValid = false;
        continue;
      }
      if (groupMap.containsKey(group.name())) {
        errors.add(new FieldProblem("optionGroups", "같은 이름의 옵션이 중복되었습니다: " + group.name()));
        definitionsValid = false;
        continue;
      }
      Set<String> seen = new HashSet<>();
      for (String value : group.values()) {
        if (!VariantCombinationKey.isValidToken(value)) {
          errors.add(new FieldProblem("optionGroups", invalidTokenMessage("옵션 값", value)));
          definitionsValid = false;
        } else if (!seen.add(value)) {
          errors.add(
              new FieldProblem(
                  "optionGroups", "옵션 '" + group.name() + "'에 같은 값이 중복되었습니다: " + value));
          definitionsValid = false;
        }
      }
      groupMap.put(group.name(), group.values());
    }
    if (!definitionsValid) {
      return;
    }

    List<String> keys = new ArrayList<>();
    Set<String> seenKeys = new HashSet<>();
    for (VariantDto variant : variants) {
      Map<String, String> optionValues = variant.optionValues();
      if (optionValues == null || !optionValues.keySet().equals(groupMap.keySet())) {
        errors.add(new FieldProblem("variants", "옵션 조합의 옵션 이름이 옵션 정의와 다릅니다: " + optionValues));
        return;
      }
      String key = VariantCombinationKey.key(List.copyOf(groupMap.keySet()), optionValues);
      if (!seenKeys.add(key)) {
        errors.add(new FieldProblem("variants", "같은 옵션 조합이 중복되었습니다: " + key));
      }
      keys.add(key);
    }

    List<String> unknown = VariantCombinationKey.unknown(groupMap, keys);
    if (!unknown.isEmpty()) {
      errors.add(new FieldProblem("variants", "옵션 정의에 없는 조합이 있습니다: " + unknown));
    }
    List<String> missing = VariantCombinationKey.missing(groupMap, keys);
    if (!missing.isEmpty()) {
      errors.add(new FieldProblem("variants", "모든 옵션 조합의 재고를 입력해야 합니다. 누락된 조합: " + missing));
    }
  }

  private static String invalidTokenMessage(String label, String value) {
    return label + "은(는) 비워 둘 수 없고 '=' 또는 '|'를 포함할 수 없습니다: " + value;
  }
}
