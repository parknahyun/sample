import { expect, test } from '@playwright/test';
import { createProductViaApi, uniqueName } from './helpers';

test('검색어·상태 필터·정렬을 적용하고 결과가 없으면 안내한다', async ({ page, request }) => {
  const prefix = uniqueName('검색테스트');
  await createProductViaApi(request, { name: `${prefix}-A`, price: 30000 });
  await createProductViaApi(request, { name: `${prefix}-B`, price: 10000 });
  await createProductViaApi(request, { name: `${prefix}-C`, price: 20000, status: 'HIDDEN' });

  await page.goto('/products');
  await page.getByLabel('검색어').fill(prefix);
  await page.getByRole('button', { name: '검색' }).click();
  const rows = page.getByRole('table', { name: '상품 목록' }).getByRole('row');
  await expect(rows).toHaveCount(1 + 3); // 머리글 + 3개

  // 가격 낮은순 정렬
  await page.getByLabel('정렬').selectOption('price,asc');
  await expect(rows.nth(1)).toContainText(`${prefix}-B`);
  await expect(rows.nth(3)).toContainText(`${prefix}-A`);

  // 상태 필터: 숨김만
  await page.getByLabel('판매 상태').selectOption('HIDDEN');
  await expect(rows).toHaveCount(1 + 1);
  await expect(rows.nth(1)).toContainText(`${prefix}-C`);

  // 일치하는 상품이 없는 검색어
  await page.getByLabel('판매 상태').selectOption('');
  await page.getByLabel('검색어').fill(`${prefix}-없는상품`);
  await page.getByRole('button', { name: '검색' }).click();
  await expect(page.getByText('결과 없음')).toBeVisible();
});
