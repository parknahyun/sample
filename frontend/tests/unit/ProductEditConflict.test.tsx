import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ConflictNotice } from '../../src/components/ConflictNotice';
import { ProductEditPage } from '../../src/pages/ProductEditPage';
import { ApiError } from '../../src/services/apiClient';
import * as productApi from '../../src/services/productApi';
import { renderWithProviders, sampleProduct } from './testUtils';

vi.mock('../../src/services/productApi');
vi.mock('../../src/services/categoryApi', () => ({
  listCategories: vi.fn().mockResolvedValue([
    { id: 1, name: '미분류' },
    { id: 2, name: '의류' },
  ]),
}));

const CONFLICT_MESSAGE = '다른 사용자가 먼저 수정했습니다. 최신 내용을 다시 불러오세요';

describe('ConflictNotice', () => {
  it('충돌 안내와 다시 불러오기 버튼을 보여 준다', async () => {
    const onReload = vi.fn();
    render(<ConflictNotice onReload={onReload} />);

    expect(screen.getByRole('alert')).toHaveTextContent(CONFLICT_MESSAGE);
    await userEvent.click(screen.getByRole('button', { name: '다시 불러오기' }));

    expect(onReload).toHaveBeenCalledTimes(1);
  });
});

describe('상품 수정 화면의 409 충돌 처리', () => {
  beforeEach(() => {
    vi.mocked(productApi.getProduct).mockReset();
    vi.mocked(productApi.updateProduct).mockReset();
  });

  function renderEdit() {
    return renderWithProviders(
      <Routes>
        <Route path="/products/:productId/edit" element={<ProductEditPage />} />
      </Routes>,
      '/products/1/edit',
    );
  }

  it('저장 시 version을 함께 보내고, 409면 충돌 안내를 띄운 뒤 다시 불러오면 최신 내용으로 바뀐다', async () => {
    vi.mocked(productApi.getProduct)
      .mockResolvedValueOnce(sampleProduct({ version: 3 }))
      .mockResolvedValue(sampleProduct({ name: '다른 사람이 바꾼 이름', version: 4 }));
    vi.mocked(productApi.updateProduct).mockRejectedValue(
      new ApiError({ status: 409, title: '충돌', detail: '이미 수정됨' }),
    );
    const user = userEvent.setup();
    renderEdit();

    const name = await screen.findByLabelText('상품명');
    await user.clear(name);
    await user.type(name, '내가 바꾼 이름');
    await user.click(screen.getByRole('button', { name: '저장' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(CONFLICT_MESSAGE);
    expect(productApi.updateProduct).toHaveBeenCalledWith(
      1,
      expect.objectContaining({ name: '내가 바꾼 이름', version: 3 }),
    );

    await user.click(screen.getByRole('button', { name: '다시 불러오기' }));

    await waitFor(() =>
      expect(screen.getByLabelText('상품명')).toHaveValue('다른 사람이 바꾼 이름'),
    );
    expect(screen.queryByText(CONFLICT_MESSAGE)).not.toBeInTheDocument();
  });

  it('판매 상태는 판매중과 숨김만 고를 수 있고 품절은 표시 전용이다', async () => {
    vi.mocked(productApi.getProduct).mockResolvedValue(sampleProduct({ status: 'SOLD_OUT' }));
    renderEdit();

    const status = await screen.findByLabelText('판매 상태');
    const options = Array.from(status.querySelectorAll('option')).map((o) => o.textContent);
    expect(options).toEqual(['판매중', '숨김']);
    expect(screen.getByText(/현재 품절 상태/)).toBeInTheDocument();
  });
});
