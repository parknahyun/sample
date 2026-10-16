import { useQuery } from '@tanstack/react-query';
import { listCategories } from '../services/categoryApi';

/** 카테고리 목록. 자주 바뀌지 않으므로 오래 캐시한다. */
export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: listCategories,
    staleTime: 5 * 60 * 1000,
  });
}
