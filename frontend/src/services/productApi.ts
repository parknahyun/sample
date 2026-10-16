import type {
  Product,
  ProductCreateRequest,
  ProductImage,
  ProductListParams,
  ProductPage,
  ProductUpdateRequest,
} from '../types/product';
import { apiRequest } from './apiClient';

export function listProducts(params: ProductListParams): Promise<ProductPage> {
  return apiRequest<ProductPage>('/products', { params: { ...params } });
}

export function getProduct(productId: number): Promise<Product> {
  return apiRequest<Product>(`/products/${productId}`);
}

export function createProduct(request: ProductCreateRequest): Promise<Product> {
  return apiRequest<Product>('/products', { method: 'POST', body: request });
}

export function updateProduct(productId: number, request: ProductUpdateRequest): Promise<Product> {
  return apiRequest<Product>(`/products/${productId}`, { method: 'PUT', body: request });
}

export function deleteProduct(productId: number): Promise<void> {
  return apiRequest<void>(`/products/${productId}`, { method: 'DELETE' });
}

export function uploadProductImage(productId: number, file: File): Promise<ProductImage> {
  const formData = new FormData();
  formData.append('file', file);
  return apiRequest<ProductImage>(`/products/${productId}/images`, { method: 'POST', formData });
}

export function deleteProductImage(productId: number, imageId: number): Promise<void> {
  return apiRequest<void>(`/products/${productId}/images/${imageId}`, { method: 'DELETE' });
}

export function setPrimaryImage(productId: number, imageId: number): Promise<void> {
  return apiRequest<void>(`/products/${productId}/images/${imageId}/primary`, { method: 'PUT' });
}
