import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { MemoryRouter, useLocation } from 'react-router';
import type { Product } from '../../src/types/product';

/** 현재 위치(경로+쿼리 문자열)를 화면에 노출해 URL 변화를 검증하게 한다. */
export function LocationDisplay() {
  const location = useLocation();
  return <div data-testid="location">{location.pathname + location.search}</div>;
}

/** 재시도 없는 QueryClient와 라우터로 감싸서 렌더링한다. */
export function renderWithProviders(ui: ReactElement, route = '/') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[route]}>
        {ui}
        <LocationDisplay />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

export const sampleCategories = [
  { id: 1, name: '미분류' },
  { id: 2, name: '의류' },
];

export function sampleProduct(overrides: Partial<Product> = {}): Product {
  return {
    id: 1,
    code: 'PRD-000001',
    name: '면 티셔츠',
    description: '부드러운 면 소재',
    price: 19000,
    categoryId: 2,
    status: 'ON_SALE',
    stockQuantity: 5,
    optionGroups: [],
    variants: [],
    images: [],
    version: 3,
    createdAt: '2026-10-07T00:00:00Z',
    updatedAt: '2026-10-07T00:00:00Z',
    ...overrides,
  };
}
