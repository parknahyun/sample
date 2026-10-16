import type { APIRequestContext } from '@playwright/test';

interface CreatedProduct {
  id: number;
  name: string;
  version: number;
  categoryId: number;
}

/** 테스트마다 겹치지 않는 상품명을 만든다. */
export function uniqueName(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1000)}`;
}

/** 사용할 수 있는 첫 번째 카테고리 ID를 가져온다. */
async function firstCategoryId(request: APIRequestContext): Promise<number> {
  const response = await request.get('/api/v1/categories');
  const categories = (await response.json()) as { id: number }[];
  return categories[0].id;
}

/** 화면을 거치지 않고 API로 상품을 준비한다. */
export async function createProductViaApi(
  request: APIRequestContext,
  fields: { name: string; price?: number; stockQuantity?: number; status?: 'ON_SALE' | 'HIDDEN' },
): Promise<CreatedProduct> {
  const response = await request.post('/api/v1/products', {
    data: {
      price: 10000,
      stockQuantity: 5,
      status: 'ON_SALE',
      ...fields,
      categoryId: await firstCategoryId(request),
    },
  });
  if (response.status() !== 201) {
    throw new Error(`상품 준비에 실패했습니다: ${response.status()} ${await response.text()}`);
  }
  return (await response.json()) as CreatedProduct;
}
