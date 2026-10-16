/** OpenAPI 계약(`product-api.openapi.yaml`)의 요청·응답 타입. */

export type ProductStatus = 'ON_SALE' | 'SOLD_OUT' | 'HIDDEN';

/** 운영자가 직접 지정할 수 있는 판매 상태. 품절(SOLD_OUT)은 재고로 결정되는 표시 전용 값이다. */
export type EditableStatus = 'ON_SALE' | 'HIDDEN';

export const STATUS_LABELS: Record<ProductStatus, string> = {
  ON_SALE: '판매중',
  SOLD_OUT: '품절',
  HIDDEN: '숨김',
};

export interface OptionGroup {
  name: string;
  values: string[];
}

export interface Variant {
  optionValues: Record<string, string>;
  stockQuantity: number;
}

export interface ProductImage {
  id: number;
  /** `/api/v1/images/{storageKey}` 형태. `<img src>`에 그대로 사용한다. */
  url: string;
  primary: boolean;
  position: number;
}

export interface Product {
  id: number;
  code: string;
  name: string;
  description?: string;
  price: number;
  categoryId: number;
  status: ProductStatus;
  stockQuantity?: number | null;
  optionGroups?: OptionGroup[];
  variants?: Variant[];
  images?: ProductImage[];
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProductSummary {
  id: number;
  code: string;
  name: string;
  price: number;
  categoryId?: number;
  status: ProductStatus;
  totalStock?: number;
  primaryImageUrl?: string | null;
  createdAt: string;
}

export interface ProductPage {
  content: ProductSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Category {
  id: number;
  name: string;
}

/** 등록·수정 공통 입력 필드. */
export interface ProductWriteFields {
  name: string;
  description?: string;
  price: number;
  categoryId: number;
  status: EditableStatus;
  /** 옵션이 없는 상품의 재고. 옵션이 있으면 지정하지 않는다. */
  stockQuantity?: number;
  optionGroups?: OptionGroup[];
  variants?: Variant[];
}

export interface ProductCreateRequest extends ProductWriteFields {
  /** 비우면 서버가 자동 생성한다. */
  code?: string;
}

export interface ProductUpdateRequest extends ProductWriteFields {
  /** 편집을 시작할 때 받은 버전. 서버의 최신 버전과 다르면 409가 반환된다. */
  version: number;
}

export const SORT_OPTIONS = [
  { value: 'createdAt,desc', label: '등록일 최신순' },
  { value: 'createdAt,asc', label: '등록일 오래된순' },
  { value: 'price,desc', label: '가격 높은순' },
  { value: 'price,asc', label: '가격 낮은순' },
] as const;

export type SortValue = (typeof SORT_OPTIONS)[number]['value'];

export interface ProductListParams {
  q?: string;
  categoryId?: number;
  status?: ProductStatus;
  sort?: SortValue;
  page?: number;
  size?: number;
}
