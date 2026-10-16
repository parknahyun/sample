import type { OptionGroup, Variant } from '../types/product';

/** 입력 중인 옵션 그룹. 값은 쉼표로 구분한 원문 텍스트를 그대로 보관한다. */
export interface OptionGroupInput {
  name: string;
  valuesText: string;
}

/** 입력 중인 옵션 조합. 재고는 입력 문자열 그대로 보관한다. */
export interface VariantInput {
  optionValues: Record<string, string>;
  stockQuantity: string;
}

export interface OptionsValue {
  groups: OptionGroupInput[];
  variants: VariantInput[];
}

export const EMPTY_OPTIONS: OptionsValue = { groups: [], variants: [] };

const STOCK_PATTERN = /^\d+$/;

export function parseValues(text: string): string[] {
  return text
    .split(',')
    .map((value) => value.trim())
    .filter((value) => value !== '');
}

/** 조합을 사람이 읽는 라벨로 만든다. 예: `색상=빨강 / 사이즈=M` */
export function variantLabel(optionValues: Record<string, string>): string {
  return Object.entries(optionValues)
    .map(([name, value]) => `${name}=${value}`)
    .join(' / ');
}

/** 이름과 값이 모두 채워진 그룹의 모든 옵션 값 조합을 만든다. 이미 입력한 재고는 같은 조합이면 유지한다. */
export function buildVariants(
  groups: OptionGroupInput[],
  previous: VariantInput[],
): VariantInput[] {
  const complete = groups
    .map((group) => ({
      name: group.name.trim(),
      values: [...new Set(parseValues(group.valuesText))],
    }))
    .filter((group) => group.name !== '' && group.values.length > 0);
  if (complete.length === 0) return [];

  const previousStock = new Map(
    previous.map((v) => [variantLabel(v.optionValues), v.stockQuantity]),
  );
  let combinations: Record<string, string>[] = [{}];
  for (const group of complete) {
    combinations = combinations.flatMap((combo) =>
      group.values.map((value) => ({ ...combo, [group.name]: value })),
    );
  }
  return combinations.map((optionValues) => ({
    optionValues,
    stockQuantity: previousStock.get(variantLabel(optionValues)) ?? '0',
  }));
}

/** 서버 응답(옵션 그룹·조합)을 편집용 입력 상태로 변환한다. */
export function toOptionsValue(groups: OptionGroup[] = [], variants: Variant[] = []): OptionsValue {
  return {
    groups: groups.map((group) => ({ name: group.name, valuesText: group.values.join(', ') })),
    variants: variants.map((variant) => ({
      optionValues: variant.optionValues,
      stockQuantity: String(variant.stockQuantity),
    })),
  };
}

/** 입력 상태를 요청 형식으로 변환한다. */
export function fromOptionsValue(options: OptionsValue): {
  groups: OptionGroup[];
  variants: Variant[];
} {
  return {
    groups: options.groups.map((group) => ({
      name: group.name.trim(),
      values: parseValues(group.valuesText),
    })),
    variants: options.variants.map((variant) => ({
      optionValues: variant.optionValues,
      stockQuantity: Number(variant.stockQuantity),
    })),
  };
}

/** 옵션 입력을 검증한다. 문제가 없으면 true, 있으면 안내 문구를 돌려준다. */
export function validateOptions(options: OptionsValue): true | string {
  const names = new Set<string>();
  for (const group of options.groups) {
    const name = group.name.trim();
    if (name === '') return '옵션 이름을 입력하세요';
    if (names.has(name)) return `중복된 옵션 이름이 있습니다: ${name}`;
    names.add(name);

    const values = parseValues(group.valuesText);
    if (values.length === 0) return `'${name}' 옵션 값을 1개 이상 입력하세요`;
    const duplicate = values.find((value, index) => values.indexOf(value) !== index);
    if (duplicate !== undefined) return `중복된 옵션 값이 있습니다: ${name} - ${duplicate}`;
  }
  if (options.variants.some((variant) => !STOCK_PATTERN.test(variant.stockQuantity))) {
    return '옵션 조합의 재고는 0 이상의 정수여야 합니다';
  }
  return true;
}

interface OptionEditorProps {
  value: OptionsValue;
  onChange: (value: OptionsValue) => void;
  disabled?: boolean;
}

/** 옵션 그룹·값 편집과 옵션 조합별 재고 표. 재고가 0인 조합은 품절로 표시한다. */
export function OptionEditor({ value, onChange, disabled }: OptionEditorProps) {
  const { groups, variants } = value;

  function changeGroups(next: OptionGroupInput[]) {
    onChange({ groups: next, variants: buildVariants(next, variants) });
  }

  function updateGroup(index: number, patch: Partial<OptionGroupInput>) {
    changeGroups(groups.map((group, i) => (i === index ? { ...group, ...patch } : group)));
  }

  function updateStock(index: number, stockQuantity: string) {
    onChange({
      groups,
      variants: variants.map((variant, i) =>
        i === index ? { ...variant, stockQuantity } : variant,
      ),
    });
  }

  return (
    <fieldset className="option-editor" disabled={disabled}>
      <legend>옵션</legend>
      {groups.map((group, index) => (
        <div className="option-group" key={index}>
          <input
            aria-label={`옵션 이름 ${index + 1}`}
            placeholder="예: 색상"
            maxLength={50}
            value={group.name}
            onChange={(event) => updateGroup(index, { name: event.target.value })}
          />
          <input
            aria-label={`옵션 값 ${index + 1}`}
            placeholder="쉼표로 구분: 빨강, 파랑"
            value={group.valuesText}
            onChange={(event) => updateGroup(index, { valuesText: event.target.value })}
          />
          <button
            type="button"
            aria-label={`옵션 그룹 ${index + 1} 삭제`}
            onClick={() => changeGroups(groups.filter((_, i) => i !== index))}
          >
            삭제
          </button>
        </div>
      ))}
      <button type="button" onClick={() => changeGroups([...groups, { name: '', valuesText: '' }])}>
        옵션 그룹 추가
      </button>

      {variants.length > 0 && (
        <table aria-label="옵션 조합별 재고">
          <thead>
            <tr>
              <th>옵션 조합</th>
              <th>재고</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            {variants.map((variant, index) => {
              const label = variantLabel(variant.optionValues);
              return (
                <tr key={label}>
                  <td>{label}</td>
                  <td>
                    <input
                      aria-label={`${label} 재고`}
                      inputMode="numeric"
                      value={variant.stockQuantity}
                      onChange={(event) => updateStock(index, event.target.value)}
                    />
                  </td>
                  <td>
                    {STOCK_PATTERN.test(variant.stockQuantity) &&
                    Number(variant.stockQuantity) === 0 ? (
                      <span className="badge sold-out">품절</span>
                    ) : null}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </fieldset>
  );
}
