import type { Category } from '../types/product';
import { apiRequest } from './apiClient';

export function listCategories(): Promise<Category[]> {
  return apiRequest<Category[]>('/categories');
}
