import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ProductListPage } from '../../src/pages/ProductListPage';
import * as productApi from '../../src/services/productApi';
import { renderWithProviders } from './testUtils';

vi.mock('../../src/services/productApi');
vi.mock('../../src/services/categoryApi', () => ({
  listCategories: vi.fn().mockResolvedValue([
    { id: 1, name: '미분류' },
    { id: 2, name: '의류' },
  ]),
}));

const summary = {
  id: 1,
  code: 'PRD-000001',
  name: '면 티셔츠',
  price: 19000,
  categoryId: 2,
  status: 'ON_SALE' as const,
  totalStock: 5,
  primaryImageUrl: null,
  createdAt: '2026-10-07T00:00:00Z',
};

function pageOf(content: (typeof summary)[], totalPages = 1) {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages };
}

function lastCallParams() {
  const calls = vi.mocked(productApi.listProducts).mock.calls;
  return calls[calls.length - 1][0];
}

describe('상품 목록의 검색·필터·정렬', () => {
  beforeEach(() => {
    vi.mocked(productApi.listProducts).mockReset();
    vi.mocked(productApi.listProducts).mockResolvedValue(pageOf([summary]));
  });

  function renderList(route = '/products') {
    return renderWithProviders(
      <Routes>
        <Route path="/products" element={<ProductListPage />} />
      </Routes>,
      route,
    );
  }

  it('기본값(등록일 내림차순, 첫 페이지)으로 조회한다', async () => {
    renderList();

    expect(await screen.findByText('면 티셔츠')).toBeInTheDocument();
    expect(lastCallParams()).toMatchObject({ sort: 'createdAt,desc', page: 0, size: 20 });
  });

  it('검색어를 제출하면 q 파라미터로 조회하고 URL에 반영한다', async () => {
    const user = userEvent.setup();
    renderList();
    await screen.findByText('면 티셔츠');

    await user.type(screen.getByLabelText('검색어'), '셔츠');
    await user.click(screen.getByRole('button', { name: '검색' }));

    await waitFor(() => expect(lastCallParams()).toMatchObject({ q: '셔츠', page: 0 }));
    expect(screen.getByTestId('location')).toHaveTextContent('q=%EC%85%94%EC%B8%A0');
  });

  it('카테고리·판매 상태·정렬을 바꾸면 해당 파라미터로 다시 조회한다', async () => {
    const user = userEvent.setup();
    renderList();
    await screen.findByText('면 티셔츠');

    await user.selectOptions(screen.getByLabelText('카테고리'), '2');
    await waitFor(() => expect(lastCallParams()).toMatchObject({ categoryId: 2 }));

    await user.selectOptions(screen.getByLabelText('판매 상태'), 'HIDDEN');
    await waitFor(() =>
      expect(lastCallParams()).toMatchObject({ categoryId: 2, status: 'HIDDEN' }),
    );

    await user.selectOptions(screen.getByLabelText('정렬'), 'price,asc');
    await waitFor(() =>
      expect(lastCallParams()).toMatchObject({
        categoryId: 2,
        status: 'HIDDEN',
        sort: 'price,asc',
        page: 0,
      }),
    );
    expect(screen.getByTestId('location')).toHaveTextContent('sort=price%2Casc');
  });

  it('조건에 맞는 상품이 없으면 "결과 없음"을 표시한다', async () => {
    vi.mocked(productApi.listProducts).mockResolvedValue(pageOf([], 0));
    renderList('/products?q=없는상품');

    expect(await screen.findByText('결과 없음')).toBeInTheDocument();
  });

  it('다음 페이지로 이동하면 page 파라미터를 바꿔 조회한다', async () => {
    vi.mocked(productApi.listProducts).mockResolvedValue(pageOf([summary], 3));
    const user = userEvent.setup();
    renderList();

    await user.click(await screen.findByRole('button', { name: '다음' }));

    await waitFor(() => expect(lastCallParams()).toMatchObject({ page: 1 }));
  });
});
