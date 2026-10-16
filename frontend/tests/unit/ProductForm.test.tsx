import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ProductForm } from '../../src/components/ProductForm';
import { sampleCategories } from './testUtils';

function renderForm(onSubmit = vi.fn()) {
  render(
    <ProductForm
      mode="create"
      categories={sampleCategories}
      submitting={false}
      onSubmit={onSubmit}
    />,
  );
  return { onSubmit, user: userEvent.setup() };
}

describe('ProductForm', () => {
  it('필수 항목(상품명·가격·카테고리)이 비어 있으면 안내하고 저장하지 않는다', async () => {
    const { onSubmit, user } = renderForm();

    await user.click(screen.getByRole('button', { name: '저장' }));

    expect(await screen.findByText('상품명을 입력하세요')).toBeInTheDocument();
    expect(screen.getByText('가격을 입력하세요')).toBeInTheDocument();
    expect(screen.getByText('카테고리를 선택하세요')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('가격 0과 재고 -1을 거부한다', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('상품명'), '티셔츠');
    await user.type(screen.getByLabelText('가격(원)'), '0');
    await user.selectOptions(screen.getByLabelText('카테고리'), '2');
    await user.type(screen.getByLabelText('재고 수량'), '-1');
    await user.click(screen.getByRole('button', { name: '저장' }));

    expect(await screen.findByText('가격은 1 이상의 정수여야 합니다')).toBeInTheDocument();
    expect(screen.getByText('재고는 0 이상의 정수여야 합니다')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('옵션이 없는 상품은 입력값을 요청 형식으로 변환해 전달한다', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('상품명'), '티셔츠');
    await user.type(screen.getByLabelText('가격(원)'), '19000');
    await user.selectOptions(screen.getByLabelText('카테고리'), '2');
    await user.type(screen.getByLabelText('재고 수량'), '7');
    await user.click(screen.getByRole('button', { name: '저장' }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
    expect(onSubmit.mock.calls[0][0]).toMatchObject({
      name: '티셔츠',
      price: 19000,
      categoryId: 2,
      status: 'ON_SALE',
      stockQuantity: 7,
    });
  });

  it('옵션 그룹을 입력하면 모든 옵션 조합의 재고 표를 만들고 재고 0은 품절로 표시한다', async () => {
    const { user } = renderForm();

    await user.click(screen.getByRole('button', { name: '옵션 그룹 추가' }));
    await user.type(screen.getByLabelText('옵션 이름 1'), '색상');
    await user.type(screen.getByLabelText('옵션 값 1'), '빨강, 파랑');
    await user.click(screen.getByRole('button', { name: '옵션 그룹 추가' }));
    await user.type(screen.getByLabelText('옵션 이름 2'), '사이즈');
    await user.type(screen.getByLabelText('옵션 값 2'), 'M, L');

    const table = screen.getByRole('table', { name: '옵션 조합별 재고' });
    expect(within(table).getAllByRole('row')).toHaveLength(1 + 4);
    // 옵션이 있으면 상품 단위 재고 입력은 사라진다.
    expect(screen.queryByLabelText('재고 수량')).not.toBeInTheDocument();

    const stock = screen.getByLabelText('색상=빨강 / 사이즈=M 재고');
    await user.clear(stock);
    await user.type(stock, '3');
    const row = stock.closest('tr') as HTMLElement;
    expect(within(row).queryByText('품절')).not.toBeInTheDocument();
    // 아직 재고를 바꾸지 않은 조합(기본 0)은 품절이다.
    expect(within(table).getAllByText('품절')).toHaveLength(3);
  });

  it('옵션 값이 중복되면 저장하지 않고 안내한다', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('상품명'), '티셔츠');
    await user.type(screen.getByLabelText('가격(원)'), '1000');
    await user.selectOptions(screen.getByLabelText('카테고리'), '1');
    await user.click(screen.getByRole('button', { name: '옵션 그룹 추가' }));
    await user.type(screen.getByLabelText('옵션 이름 1'), '색상');
    await user.type(screen.getByLabelText('옵션 값 1'), '빨강, 빨강');
    await user.click(screen.getByRole('button', { name: '저장' }));

    expect(await screen.findByText(/중복된 옵션 값/)).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });
});
