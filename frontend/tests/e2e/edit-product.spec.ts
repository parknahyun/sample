import { expect, test } from '@playwright/test';
import { createProductViaApi, uniqueName } from './helpers';

test('가격 수정, 재고 0 품절 표시, 재고 보충 시 판매중 복귀', async ({ page, request }) => {
  const product = await createProductViaApi(request, {
    name: uniqueName('수정테스트'),
    price: 10000,
  });
  const editUrl = `/products/${product.id}/edit`;

  // 가격 수정
  await page.goto(editUrl);
  await page.getByLabel('가격(원)').fill('25000');
  await page.getByRole('button', { name: '저장' }).click();
  await expect(page).toHaveURL(`/products/${product.id}`);
  await expect(page.getByText('25,000원')).toBeVisible();

  // 재고 0 -> 품절
  await page.goto(editUrl);
  await page.getByLabel('재고 수량').fill('0');
  await page.getByRole('button', { name: '저장' }).click();
  await expect(page).toHaveURL(`/products/${product.id}`);
  await expect(page.getByText('품절')).toBeVisible();

  // 재고 보충 -> 판매중 복귀
  await page.goto(editUrl);
  await page.getByLabel('재고 수량').fill('3');
  await page.getByRole('button', { name: '저장' }).click();
  await expect(page).toHaveURL(`/products/${product.id}`);
  await expect(page.getByText('판매중')).toBeVisible();
});

test('다른 사용자가 먼저 수정하면 충돌 안내와 다시 불러오기를 제공한다', async ({
  page,
  request,
}) => {
  const product = await createProductViaApi(request, { name: uniqueName('충돌테스트') });

  await page.goto(`/products/${product.id}/edit`);
  await expect(page.getByLabel('상품명')).toHaveValue(product.name);

  // 화면을 연 뒤 다른 사용자가 같은 버전으로 먼저 저장한 상황을 만든다.
  const other = await request.put(`/api/v1/products/${product.id}`, {
    data: {
      name: `${product.name}-다른사용자`,
      price: 10000,
      categoryId: product.categoryId,
      stockQuantity: 5,
      version: product.version,
    },
  });
  expect(other.ok()).toBeTruthy();

  await page.getByLabel('가격(원)').fill('12000');
  await page.getByRole('button', { name: '저장' }).click();

  await expect(
    page.getByText('다른 사용자가 먼저 수정했습니다. 최신 내용을 다시 불러오세요'),
  ).toBeVisible();

  await page.getByRole('button', { name: '다시 불러오기' }).click();
  await expect(page.getByLabel('상품명')).toHaveValue(`${product.name}-다른사용자`);
});
