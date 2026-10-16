import type { ReactNode } from 'react';
import { Controller, useForm, useWatch } from 'react-hook-form';
import type { ApiFieldError } from '../services/apiClient';
import type { Category, EditableStatus, Product, ProductCreateRequest } from '../types/product';
import {
  EMPTY_OPTIONS,
  OptionEditor,
  fromOptionsValue,
  toOptionsValue,
  validateOptions,
  type OptionsValue,
} from './OptionEditor';

interface FormValues {
  code: string;
  name: string;
  description: string;
  price: string;
  categoryId: string;
  status: EditableStatus;
  stockQuantity: string;
  options: OptionsValue;
}

interface ProductFormProps {
  mode: 'create' | 'edit';
  categories: Category[];
  /** 수정 화면에서 채워 넣을 기존 상품 */
  initial?: Product;
  submitting: boolean;
  disabled?: boolean;
  /** 서버가 돌려준 필드별 검증 오류 */
  serverErrors?: ApiFieldError[];
  /** 저장 요청 본문(등록·수정 공통 필드). 수정이면 호출한 쪽에서 version을 덧붙인다. */
  onSubmit: (request: ProductCreateRequest) => void;
  /** 저장 버튼 바로 위에 끼워 넣을 영역(이미지 등) */
  children?: ReactNode;
}

const INTEGER_PATTERN = /^-?\d+$/;

function toDefaultValues(initial?: Product): FormValues {
  return {
    code: initial?.code ?? '',
    name: initial?.name ?? '',
    description: initial?.description ?? '',
    price: initial ? String(initial.price) : '',
    categoryId: initial ? String(initial.categoryId) : '',
    // 품절은 표시 전용이므로 선택값으로는 판매중으로 둔다(재고로 결정됨).
    status: initial?.status === 'HIDDEN' ? 'HIDDEN' : 'ON_SALE',
    stockQuantity:
      initial?.stockQuantity !== undefined && initial.stockQuantity !== null
        ? String(initial.stockQuantity)
        : '',
    options: initial ? toOptionsValue(initial.optionGroups, initial.variants) : EMPTY_OPTIONS,
  };
}

function toRequest(values: FormValues, mode: 'create' | 'edit'): ProductCreateRequest {
  const hasOptions = values.options.groups.length > 0;
  const request: ProductCreateRequest = {
    name: values.name.trim(),
    description: values.description,
    price: Number(values.price),
    categoryId: Number(values.categoryId),
    status: values.status,
  };
  if (hasOptions) {
    const { groups, variants } = fromOptionsValue(values.options);
    request.optionGroups = groups;
    request.variants = variants;
  } else {
    request.stockQuantity = values.stockQuantity.trim() === '' ? 0 : Number(values.stockQuantity);
  }
  if (mode === 'create' && values.code.trim() !== '') request.code = values.code.trim();
  return request;
}

/** 상품 등록·수정 공통 폼. 입력 검증은 클라이언트에서 먼저 하고, 서버 검증 오류도 함께 보여 준다. */
export function ProductForm({
  mode,
  categories,
  initial,
  submitting,
  disabled,
  serverErrors = [],
  onSubmit,
  children,
}: ProductFormProps) {
  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ defaultValues: toDefaultValues(initial) });
  const options = useWatch({ control, name: 'options' });
  const hasOptions = options.groups.length > 0;
  const locked = submitting || disabled;

  return (
    <form noValidate onSubmit={handleSubmit((values) => onSubmit(toRequest(values, mode)))}>
      {serverErrors.length > 0 && (
        <ul className="field-error" role="alert">
          {serverErrors.map((error) => (
            <li key={`${error.field}-${error.message}`}>
              {error.field}: {error.message}
            </li>
          ))}
        </ul>
      )}

      <fieldset disabled={locked}>
        <div className="field">
          <label htmlFor="code">상품 코드{mode === 'create' ? '(선택)' : ''}</label>
          <input
            id="code"
            placeholder="비우면 자동 생성됩니다"
            readOnly={mode === 'edit'}
            {...register('code', {
              pattern: {
                value: /^$|^[A-Z0-9-]{3,30}$/,
                message: '상품 코드는 영문 대문자·숫자·하이픈 3~30자여야 합니다',
              },
            })}
          />
          {errors.code && <p className="field-error">{errors.code.message}</p>}
        </div>

        <div className="field">
          <label htmlFor="name">상품명</label>
          <input
            id="name"
            maxLength={200}
            {...register('name', { validate: (v) => v.trim() !== '' || '상품명을 입력하세요' })}
          />
          {errors.name && <p className="field-error">{errors.name.message}</p>}
        </div>

        <div className="field">
          <label htmlFor="description">설명</label>
          <textarea id="description" rows={4} {...register('description')} />
        </div>

        <div className="field">
          <label htmlFor="price">가격(원)</label>
          <input
            id="price"
            inputMode="numeric"
            {...register('price', {
              validate: (v) => {
                if (v.trim() === '') return '가격을 입력하세요';
                return (
                  (INTEGER_PATTERN.test(v.trim()) && Number(v) >= 1) ||
                  '가격은 1 이상의 정수여야 합니다'
                );
              },
            })}
          />
          {errors.price && <p className="field-error">{errors.price.message}</p>}
        </div>

        <div className="field">
          <label htmlFor="categoryId">카테고리</label>
          <select
            id="categoryId"
            {...register('categoryId', { validate: (v) => v !== '' || '카테고리를 선택하세요' })}
          >
            <option value="">선택하세요</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
          {errors.categoryId && <p className="field-error">{errors.categoryId.message}</p>}
        </div>

        <div className="field">
          <label htmlFor="status">판매 상태</label>
          <select id="status" {...register('status')}>
            <option value="ON_SALE">판매중</option>
            <option value="HIDDEN">숨김</option>
          </select>
          {initial?.status === 'SOLD_OUT' && (
            <p className="hint">
              현재 품절 상태입니다. 재고가 1 이상으로 보충되면 자동으로 판매중이 됩니다.
            </p>
          )}
        </div>

        {!hasOptions && (
          <div className="field">
            <label htmlFor="stockQuantity">재고 수량</label>
            <input
              id="stockQuantity"
              inputMode="numeric"
              placeholder="비우면 0"
              {...register('stockQuantity', {
                validate: (v) =>
                  v.trim() === '' ||
                  (INTEGER_PATTERN.test(v.trim()) && Number(v) >= 0) ||
                  '재고는 0 이상의 정수여야 합니다',
              })}
            />
            {errors.stockQuantity && <p className="field-error">{errors.stockQuantity.message}</p>}
          </div>
        )}

        <Controller
          control={control}
          name="options"
          rules={{ validate: validateOptions }}
          render={({ field }) => <OptionEditor value={field.value} onChange={field.onChange} />}
        />
        {errors.options && <p className="field-error">{errors.options.message}</p>}
      </fieldset>

      {children}

      <div className="actions">
        <button type="submit" disabled={locked}>
          저장
        </button>
      </div>
    </form>
  );
}
