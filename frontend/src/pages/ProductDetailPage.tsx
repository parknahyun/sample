import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { DeleteConfirmDialog } from '../components/DeleteConfirmDialog';
import { ProductNotFound } from '../components/ProductNotFound';
import { variantLabel } from '../components/OptionEditor';
import { formatDate, formatPrice } from '../format';
import { useCategories } from '../hooks/useCategories';
import { useDeleteProduct, useProduct } from '../hooks/useProducts';
import { ApiError } from '../services/apiClient';
import { STATUS_LABELS } from '../types/product';

/** 상품 상세. 재고가 0인 옵션 조합은 품절로 표시하고, 삭제는 확인 절차를 거친다. */
export function ProductDetailPage() {
  const productId = Number(useParams().productId);
  const navigate = useNavigate();
  const product = useProduct(productId);
  const categories = useCategories();
  const deletion = useDeleteProduct();
  const [confirming, setConfirming] = useState(false);

  if (product.isPending) return <p>불러오는 중...</p>;
  if (product.isError) {
    if (product.error instanceof ApiError && product.error.status === 404) {
      return <ProductNotFound />;
    }
    return (
      <p className="field-error" role="alert">
        {product.error instanceof ApiError ? product.error.detail : '상품을 불러오지 못했습니다.'}
      </p>
    );
  }

  const data = product.data;
  const categoryName =
    categories.data?.find((c) => c.id === data.categoryId)?.name ?? data.categoryId;
  const hasOptions = (data.variants?.length ?? 0) > 0;

  function confirmDelete() {
    deletion.mutate(productId, {
      onSuccess: () => navigate('/products'),
      onSettled: () => setConfirming(false),
    });
  }

  if (deletion.isError && deletion.error instanceof ApiError && deletion.error.status === 404) {
    return <ProductNotFound />;
  }

  return (
    <section>
      <div className="page-header">
        <h1>{data.name}</h1>
        <div className="actions">
          <Link className="button" to={`/products/${data.id}/edit`}>
            수정
          </Link>
          <button type="button" className="danger" onClick={() => setConfirming(true)}>
            삭제
          </button>
        </div>
      </div>

      {deletion.isError && (
        <p className="field-error" role="alert">
          {deletion.error instanceof ApiError ? deletion.error.detail : '삭제하지 못했습니다.'}
        </p>
      )}

      <dl className="detail">
        <dt>상품 코드</dt>
        <dd>{data.code}</dd>
        <dt>카테고리</dt>
        <dd>{categoryName}</dd>
        <dt>가격</dt>
        <dd>{formatPrice(data.price)}</dd>
        <dt>판매 상태</dt>
        <dd>{STATUS_LABELS[data.status]}</dd>
        {!hasOptions && (
          <>
            <dt>재고</dt>
            <dd>{data.stockQuantity ?? 0}</dd>
          </>
        )}
        <dt>설명</dt>
        <dd>{data.description || '-'}</dd>
        <dt>등록일</dt>
        <dd>{formatDate(data.createdAt)}</dd>
        <dt>수정일</dt>
        <dd>{formatDate(data.updatedAt)}</dd>
      </dl>

      {hasOptions && (
        <>
          <h2>옵션 조합별 재고</h2>
          <table aria-label="옵션 조합별 재고">
            <thead>
              <tr>
                <th>옵션 조합</th>
                <th>재고</th>
                <th>상태</th>
              </tr>
            </thead>
            <tbody>
              {data.variants?.map((variant) => {
                const label = variantLabel(variant.optionValues);
                return (
                  <tr key={label}>
                    <td>{label}</td>
                    <td>{variant.stockQuantity}</td>
                    <td>
                      {variant.stockQuantity === 0 && <span className="badge sold-out">품절</span>}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </>
      )}

      <h2>이미지</h2>
      {(data.images?.length ?? 0) === 0 ? (
        <p className="hint">등록된 이미지가 없습니다.</p>
      ) : (
        <ul className="image-list">
          {data.images?.map((image) => (
            <li key={image.id}>
              <img src={image.url} alt={`${data.name} 이미지 ${image.position + 1}`} width={120} />
              {image.primary && <span className="badge">대표</span>}
            </li>
          ))}
        </ul>
      )}

      <p>
        <Link to="/products">목록으로</Link>
      </p>

      <DeleteConfirmDialog
        open={confirming}
        productName={data.name}
        pending={deletion.isPending}
        onConfirm={confirmDelete}
        onCancel={() => setConfirming(false)}
      />
    </section>
  );
}
