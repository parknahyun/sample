package com.example.commerce.product.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 옵션 그룹(예: 색상)과 그 옵션 값 목록. */
public record OptionGroupDto(
    @NotBlank(message = "옵션 이름은 필수입니다") @Size(max = 50, message = "옵션 이름은 50자 이하여야 합니다")
        String name,
    @NotEmpty(message = "옵션 값은 1개 이상 필요합니다")
        List<
                @NotBlank(message = "옵션 값은 비워 둘 수 없습니다")
                @Size(max = 50, message = "옵션 값은 50자 이하여야 합니다") String>
            values) {}
