import { useState, type FormEvent } from 'react';
import {
  SORT_OPTIONS,
  STATUS_LABELS,
  type Category,
  type ProductStatus,
  type SortValue,
} from '../types/product';

export interface FilterValues {
  q: string;
  categoryId?: number;
  status?: ProductStatus;
  sort: SortValue;
}

interface ProductFiltersProps {
  values: FilterValues;
  categories: Category[];
  onChange: (patch: Partial<FilterValues>) => void;
}

const STATUS_OPTIONS: ProductStatus[] = ['ON_SALE', 'SOLD_OUT', 'HIDDEN'];

/** 검색어 입력, 카테고리·판매 상태 선택, 정렬 선택. 검색어는 검색 버튼으로 확정한다. */
export function ProductFilters({ values, categories, onChange }: ProductFiltersProps) {
  const [draft, setDraft] = useState(values.q);

  function submitSearch(event: FormEvent) {
    event.preventDefault();
    onChange({ q: draft.trim() });
  }

  return (
    <div className="filters">
      <form onSubmit={submitSearch} role="search">
        <label htmlFor="filter-q">검색어</label>
        <input
          id="filter-q"
          placeholder="상품명"
          maxLength={200}
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
        />
        <button type="submit">검색</button>
      </form>

      <label htmlFor="filter-category">카테고리</label>
      <select
        id="filter-category"
        value={values.categoryId ?? ''}
        onChange={(event) =>
          onChange({ categoryId: event.target.value ? Number(event.target.value) : undefined })
        }
      >
        <option value="">전체</option>
        {categories.map((category) => (
          <option key={category.id} value={category.id}>
            {category.name}
          </option>
        ))}
      </select>

      <label htmlFor="filter-status">판매 상태</label>
      <select
        id="filter-status"
        value={values.status ?? ''}
        onChange={(event) =>
          onChange({ status: (event.target.value || undefined) as ProductStatus | undefined })
        }
      >
        <option value="">전체</option>
        {STATUS_OPTIONS.map((status) => (
          <option key={status} value={status}>
            {STATUS_LABELS[status]}
          </option>
        ))}
      </select>

      <label htmlFor="filter-sort">정렬</label>
      <select
        id="filter-sort"
        value={values.sort}
        onChange={(event) => onChange({ sort: event.target.value as SortValue })}
      >
        {SORT_OPTIONS.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </div>
  );
}
