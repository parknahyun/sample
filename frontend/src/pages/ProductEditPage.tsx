import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { ConflictNotice } from '../components/ConflictNotice';
import { ImageUploader, MAX_IMAGES } from '../components/ImageUploader';
import { ProductForm } from '../components/ProductForm';
import { ProductNotFound } from '../components/ProductNotFound';
import { useCategories } from '../hooks/useCategories';
import { useImageActions, useProduct, useUpdateProduct } from '../hooks/useProducts';
import { uploadImagesSequentially } from '../hooks/uploadImages';
import { ApiError } from '../services/apiClient';
import type { ProductCreateRequest } from '../types/product';

/** 상품 수정. `version`을 함께 보내고, 409면 최신 내용을 다시 불러오도록 안내한다. */
export function ProductEditPage() {
  const productId = Number(useParams().productId);
  const navigate = useNavigate();
  const product = useProduct(productId);
  const categories = useCategories();
  const update = useUpdateProduct(productId);
  const imageActions = useImageActions(productId);
  const [conflict, setConflict] = useState(false);
  const [files, setFiles] = useState<File[]>([]);
  const [uploading, setUploading] = useState(false);
  const [uploadFailed, setUploadFailed] = useState(false);

  if (product.isPending || categories.isPending) return <p>불러오는 중...</p>;
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
  const images = data.images ?? [];
  const error = update.error instanceof ApiError ? update.error : null;
  const otherError = update.isError && error?.status !== 409;

  async function handleSubmit(request: ProductCreateRequest) {
    setConflict(false);
    setUploadFailed(false);
    try {
      await update.mutateAsync({ ...request, version: data.version });
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 409) setConflict(true);
      return;
    }
    if (files.length > 0) {
      setUploading(true);
      const result = await uploadImagesSequentially(productId, files);
      setUploading(false);
      if (result.failed) {
        setUploadFailed(true);
        return;
      }
    }
    navigate(`/products/${productId}`);
  }

  async function reload() {
    setConflict(false);
    update.reset();
    await product.refetch();
  }

  return (
    <section>
      <h1>상품 수정</h1>

      {conflict && <ConflictNotice onReload={reload} />}
      {otherError && (
        <p className="field-error" role="alert">
          {error?.detail ?? '상품을 저장하지 못했습니다. 잠시 후 다시 시도하세요.'}
        </p>
      )}
      {uploadFailed && (
        <p className="field-error" role="alert">
          상품은 저장되었지만 이미지를 올리지 못했습니다. 파일을 확인하고 다시 올려 주세요.
        </p>
      )}

      <ProductForm
        // 다시 불러온 최신 내용으로 폼을 초기화하기 위해 조회 시각을 키에 넣는다.
        key={`${data.version}-${product.dataUpdatedAt}`}
        mode="edit"
        categories={categories.data ?? []}
        initial={data}
        submitting={update.isPending || uploading}
        serverErrors={error?.errors}
        onSubmit={handleSubmit}
      >
        <fieldset>
          <legend>등록된 이미지</legend>
          {images.length === 0 && <p className="hint">등록된 이미지가 없습니다.</p>}
          <ul className="image-list">
            {images.map((image) => (
              <li key={image.id}>
                <img src={image.url} alt={`이미지 ${image.position + 1}`} width={80} height={80} />
                {image.primary ? (
                  <span className="badge">대표</span>
                ) : (
                  <button type="button" onClick={() => imageActions.makePrimary.mutate(image.id)}>
                    대표로 지정
                  </button>
                )}
                <button type="button" onClick={() => imageActions.remove.mutate(image.id)}>
                  이미지 삭제
                </button>
              </li>
            ))}
          </ul>
        </fieldset>
        <ImageUploader files={files} onChange={setFiles} remaining={MAX_IMAGES - images.length} />
      </ProductForm>

      <p>
        <Link to={`/products/${productId}`}>상세로 돌아가기</Link>
      </p>
    </section>
  );
}
