import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DeleteConfirmDialog } from '../../src/components/DeleteConfirmDialog';
import { ProductDetailPage } from '../../src/pages/ProductDetailPage';
import * as productApi from '../../src/services/productApi';
import { renderWithProviders, sampleProduct } from './testUtils';

vi.mock('../../src/services/productApi');
vi.mock('../../src/services/categoryApi', () => ({
  listCategories: vi.fn().mockResolvedValue([
    { id: 1, name: '미분류' },
    { id: 2, name: '의류' },
  ]),
}));

describe('DeleteConfirmDialog', () => {
  it('열려 있으면 상품명을 담은 확인 문구를 보여 주고, 닫혀 있으면 그리지 않는다', () => {
    const { rerender } = render(
      <DeleteConfirmDialog open productName="티셔츠" onConfirm={vi.fn()} onCancel={vi.fn()} />,
    );
    expect(screen.getByRole('dialog')).toHaveTextContent('티셔츠');

    rerender(
      <DeleteConfirmDialog
        open={false}
        productName="티셔츠"
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('취소하면 onCancel만 호출한다', async () => {
    const onConfirm = vi.fn();
    const onCancel = vi.fn();
    const user = userEvent.setup();
    render(
      <DeleteConfirmDialog open productName="티셔츠" onConfirm={onConfirm} onCancel={onCancel} />,
    );

    await user.click(screen.getByRole('button', { name: '취소' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();
  });
});

describe('상품 상세 화면의 삭제 확인 절차', () => {
  beforeEach(() => {
    vi.mocked(productApi.getProduct).mockResolvedValue(sampleProduct());
    vi.mocked(productApi.deleteProduct).mockResolvedValue(undefined);
  });

  function renderDetail() {
    return renderWithProviders(
      <Routes>
        <Route path="/products" element={<div>목록 화면</div>} />
        <Route path="/products/:productId" element={<ProductDetailPage />} />
      </Routes>,
      '/products/1',
    );
  }

  it('삭제를 누르면 확인 대화상자가 뜨고, 취소하면 삭제 요청을 보내지 않는다', async () => {
    const user = userEvent.setup();
    renderDetail();

    await user.click(await screen.findByRole('button', { name: '삭제' }));
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(productApi.deleteProduct).not.toHaveBeenCalled();

    await user.click(screen.getByRole('button', { name: '취소' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(productApi.deleteProduct).not.toHaveBeenCalled();
  });

  it('확인하면 삭제 요청을 보내고 목록 화면으로 이동한다', async () => {
    const user = userEvent.setup();
    renderDetail();

    await user.click(await screen.findByRole('button', { name: '삭제' }));
    await user.click(screen.getByRole('button', { name: '삭제 확인' }));

    await waitFor(() => expect(productApi.deleteProduct).toHaveBeenCalledWith(1));
    expect(await screen.findByText('목록 화면')).toBeInTheDocument();
  });

  it('존재하지 않는 상품이면 안내를 표시한다', async () => {
    const { ApiError } = await import('../../src/services/apiClient');
    vi.mocked(productApi.getProduct).mockRejectedValue(
      new ApiError({ status: 404, title: '찾을 수 없음', detail: '상품이 없습니다' }),
    );
    renderDetail();

    expect(await screen.findByText('존재하지 않는 상품입니다')).toBeInTheDocument();
  });
});
