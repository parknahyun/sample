import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createProduct,
  deleteProduct,
  deleteProductImage,
  getProduct,
  listProducts,
  setPrimaryImage,
  updateProduct,
} from '../services/productApi';
import type {
  ProductCreateRequest,
  ProductListParams,
  ProductUpdateRequest,
} from '../types/product';

export function useProducts(params: ProductListParams) {
  return useQuery({
    queryKey: ['products', 'list', params],
    queryFn: () => listProducts(params),
  });
}

export function useProduct(productId: number) {
  return useQuery({
    queryKey: ['products', 'detail', productId],
    queryFn: () => getProduct(productId),
    // 수정 중인 폼이 창 전환만으로 초기화되지 않도록 자동 재조회를 끈다.
    refetchOnWindowFocus: false,
  });
}

export function useCreateProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: ProductCreateRequest) => createProduct(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['products', 'list'] }),
  });
}

export function useUpdateProduct(productId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: ProductUpdateRequest) => updateProduct(productId, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['products'] }),
  });
}

export function useDeleteProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (productId: number) => deleteProduct(productId),
    onSuccess: (_data, productId) => {
      queryClient.removeQueries({ queryKey: ['products', 'detail', productId] });
      return queryClient.invalidateQueries({ queryKey: ['products', 'list'] });
    },
  });
}

/** 수정 화면의 기존 이미지 삭제·대표 지정. 끝나면 상품 상세를 다시 불러온다. */
export function useImageActions(productId: number) {
  const queryClient = useQueryClient();
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['products'] });
  const remove = useMutation({
    mutationFn: (imageId: number) => deleteProductImage(productId, imageId),
    onSuccess: refresh,
  });
  const makePrimary = useMutation({
    mutationFn: (imageId: number) => setPrimaryImage(productId, imageId),
    onSuccess: refresh,
  });
  return { remove, makePrimary };
}
