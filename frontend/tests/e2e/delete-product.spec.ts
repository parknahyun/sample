import { expect, test } from '@playwright/test';
import { createProductViaApi, uniqueName } from './helpers';

test('삭제를 확인하면 목록에서 사라지고 상세 접근은 존재하지 않는 상품으로 안내한다', async ({
  page,
  request,
}) => {
  const product = await createProductViaApi(request, { name: uniqueName('삭제테스트') });

  await page.goto(`/products/${product.id}`);
  await page.getByRole('button', { name: '삭제' }).click();

  // 취소하면 상품이 남아 있다.
  await expect(page.getByRole('dialog')).toBeVisible();
  await page.getByRole('button', { name: '취소' }).click();
  await expect(page.getByRole('heading', { name: product.name })).toBeVisible();

  // 확인하면 목록으로 이동하고 검색 결과에서 사라진다.
  await page.getByRole('button', { name: '삭제' }).click();
  await page.getByRole('button', { name: '삭제 확인' }).click();
  await expect(page).toHaveURL(/\/products$/);
  await page.getByLabel('검색어').fill(product.name);
  await page.getByRole('button', { name: '검색' }).click();
  await expect(page.getByText('결과 없음')).toBeVisible();

  // 삭제된 상품의 상세에 접근하면 404 안내가 나온다.
  await page.goto(`/products/${product.id}`);
  await expect(page.getByText('존재하지 않는 상품입니다')).toBeVisible();
});
