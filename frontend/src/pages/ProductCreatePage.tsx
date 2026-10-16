import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { ImageUploader } from '../components/ImageUploader';
import { ProductForm } from '../components/ProductForm';
import { useCategories } from '../hooks/useCategories';
import { useCreateProduct } from '../hooks/useProducts';
import { uploadImagesSequentially } from '../hooks/uploadImages';
import { ApiError } from '../services/apiClient';
import type { ProductCreateRequest } from '../types/product';

/** 상품 등록. 등록 성공 후 선택한 이미지를 별도 단계로 올린다. */
export function ProductCreatePage() {
  const navigate = useNavigate();
  const categories = useCategories();
  const create = useCreateProduct();
  const [files, setFiles] = useState<File[]>([]);
  const [uploading, setUploading] = useState(false);
  // 등록은 됐지만 이미지 업로드가 실패한 상품의 ID
  const [uploadFailedFor, setUploadFailedFor] = useState<number | null>(null);

  async function handleSubmit(request: ProductCreateRequest) {
    let created;
    try {
      created = await create.mutateAsync(request);
    } catch {
      return; // 오류 안내는 create.error로 표시한다.
    }
    if (files.length === 0) {
      navigate(`/products/${created.id}`);
      return;
    }
    setUploading(true);
    const result = await uploadImagesSequentially(created.id, files);
    setUploading(false);
    if (result.failed) setUploadFailedFor(created.id);
    else navigate(`/products/${created.id}`);
  }

  if (categories.isPending) return <p>불러오는 중...</p>;

  const error = create.error instanceof ApiError ? create.error : null;

  return (
    <section>
      <h1>상품 등록</h1>

      {create.isError && (
        <p className="field-error" role="alert">
          {error?.detail ?? '상품을 등록하지 못했습니다. 잠시 후 다시 시도하세요.'}
        </p>
      )}

      {uploadFailedFor !== null && (
        <div className="notice notice-warning" role="alert">
          <p>상품은 등록되었지만 이미지를 올리지 못했습니다. 수정 화면에서 다시 올려 주세요.</p>
          <Link to={`/products/${uploadFailedFor}/edit`}>수정 화면으로 이동</Link>
        </div>
      )}

      <ProductForm
        mode="create"
        categories={categories.data ?? []}
        submitting={create.isPending || uploading}
        disabled={uploadFailedFor !== null}
        serverErrors={error?.errors}
        onSubmit={handleSubmit}
      >
        <ImageUploader files={files} onChange={setFiles} disabled={uploadFailedFor !== null} />
      </ProductForm>
    </section>
  );
}
