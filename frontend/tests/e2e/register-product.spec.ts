import { expect, test } from '@playwright/test';
import { uniqueName } from './helpers';

test('상품을 등록하고 목록과 상세에서 같은 정보를 확인한다', async ({ page }) => {
  const name = uniqueName('등록테스트');

  await page.goto('/products/new');
  await page.getByLabel('상품명').fill(name);
  await page.getByLabel('설명').fill('E2E로 등록한 상품');
  await page.getByLabel('가격(원)').fill('19000');
  await page.getByLabel('카테고리').selectOption({ index: 1 });
  await page.getByLabel('재고 수량').fill('10');
  await page.getByRole('button', { name: '저장' }).click();

  // 등록 후 상세 화면으로 이동한다.
  await expect(page).toHaveURL(/\/products\/\d+$/);
  await expect(page.getByRole('heading', { name })).toBeVisible();
  await expect(page.getByText('19,000원')).toBeVisible();
  await expect(page.getByText('PRD-')).toBeVisible();

  // 목록에서 검색해 같은 상품을 찾고 상세로 다시 들어간다.
  await page.goto('/products');
  await page.getByLabel('검색어').fill(name);
  await page.getByRole('button', { name: '검색' }).click();
  await page.getByRole('link', { name }).click();

  await expect(page.getByRole('heading', { name })).toBeVisible();
  await expect(page.getByText('E2E로 등록한 상품')).toBeVisible();
});

test('필수 항목을 비우면 누락 안내가 표시된다', async ({ page }) => {
  await page.goto('/products/new');
  await page.getByRole('button', { name: '저장' }).click();

  await expect(page.getByText('상품명을 입력하세요')).toBeVisible();
  await expect(page.getByText('가격을 입력하세요')).toBeVisible();
  await expect(page.getByText('카테고리를 선택하세요')).toBeVisible();
});
