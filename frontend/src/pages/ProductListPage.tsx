import { Link, useSearchParams } from 'react-router';
import { ProductFilters, type FilterValues } from '../components/ProductFilters';
import { formatDate, formatPrice } from '../format';
import { useCategories } from '../hooks/useCategories';
import { useProducts } from '../hooks/useProducts';
import { ApiError } from '../services/apiClient';
import {
  SORT_OPTIONS,
  STATUS_LABELS,
  type ProductListParams,
  type ProductStatus,
  type SortValue,
} from '../types/product';

const PAGE_SIZE = 20;
const DEFAULT_SORT: SortValue = 'createdAt,desc';

function parseParams(searchParams: URLSearchParams): ProductListParams {
  const categoryId = Number(searchParams.get('categoryId'));
  const status = searchParams.get('status');
  const sort = searchParams.get('sort');
  const page = Number(searchParams.get('page'));
  return {
    q: searchParams.get('q') || undefined,
    categoryId: Number.isInteger(categoryId) && categoryId > 0 ? categoryId : undefined,
    status: status && status in STATUS_LABELS ? (status as ProductStatus) : undefined,
    sort: SORT_OPTIONS.find((option) => option.value === sort)?.value ?? DEFAULT_SORT,
    page: Number.isInteger(page) && page > 0 ? page : 0,
    size: PAGE_SIZE,
  };
}

/** 상품 목록. 검색·필터·정렬·페이지는 URL 쿼리 파라미터로 관리한다. */
export function ProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const params = parseParams(searchParams);
  const products = useProducts(params);
  const categories = useCategories();

  function updateParams(patch: Partial<ProductListParams>) {
    const next = { ...params, ...patch };
    const query = new URLSearchParams();
    for (const [key, value] of Object.entries(next)) {
      if (key === 'size' || value === undefined || value === '') continue;
      query.set(key, String(value));
    }
    setSearchParams(query);
  }

  // 조건이 바뀌면 첫 페이지부터 다시 본다.
  function changeFilters(patch: Partial<FilterValues>) {
    updateParams({ ...patch, page: 0 });
  }

  const filterValues: FilterValues = {
    q: params.q ?? '',
    categoryId: params.categoryId,
    status: params.status,
    sort: params.sort ?? DEFAULT_SORT,
  };

  return (
    <section>
      <div className="page-header">
        <h1>상품 목록</h1>
        <Link className="button" to="/products/new">
          상품 등록
        </Link>
      </div>

      <ProductFilters
        // 주소창으로 검색어가 바뀐 경우 입력창도 맞춘다.
        key={filterValues.q}
        values={filterValues}
        categories={categories.data ?? []}
        onChange={changeFilters}
      />

      {products.isPending && <p>불러오는 중...</p>}
      {products.isError && (
        <p className="field-error" role="alert">
          {products.error instanceof ApiError
            ? products.error.detail
            : '상품 목록을 불러오지 못했습니다.'}
        </p>
      )}

      {products.data && products.data.content.length === 0 && <p className="empty">결과 없음</p>}

      {products.data && products.data.content.length > 0 && (
        <>
          <table aria-label="상품 목록">
            <thead>
              <tr>
                <th>이미지</th>
                <th>코드</th>
                <th>상품명</th>
                <th>가격</th>
                <th>재고</th>
                <th>상태</th>
                <th>등록일</th>
              </tr>
            </thead>
            <tbody>
              {products.data.content.map((product) => (
                <tr key={product.id}>
                  <td>
                    {product.primaryImageUrl && (
                      <img src={product.primaryImageUrl} alt="" width={48} height={48} />
                    )}
                  </td>
                  <td>{product.code}</td>
                  <td>
                    <Link to={`/products/${product.id}`}>{product.name}</Link>
                  </td>
                  <td>{formatPrice(product.price)}</td>
                  <td>{product.totalStock ?? '-'}</td>
                  <td>{STATUS_LABELS[product.status]}</td>
                  <td>{formatDate(product.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <nav className="pagination" aria-label="페이지 이동">
            <button
              type="button"
              disabled={products.data.page <= 0}
              onClick={() => updateParams({ page: products.data.page - 1 })}
            >
              이전
            </button>
            <span>
              {products.data.page + 1} / {products.data.totalPages} 페이지 (총{' '}
              {products.data.totalElements}개)
            </span>
            <button
              type="button"
              disabled={products.data.page + 1 >= products.data.totalPages}
              onClick={() => updateParams({ page: products.data.page + 1 })}
            >
              다음
            </button>
          </nav>
        </>
      )}
    </section>
  );
}
