package com.example.commerce.product.domain;

import java.util.Collection;

/**
 * 판매 상태 결정 규칙(FR-008, FR-009, FR-016).
 *
 * <ul>
 *   <li>숨김은 재고와 무관하게 유지한다.
 *   <li>그 밖에는 재고 합계가 0이면 품절, 1 이상이면 판매중이다(품절 상품은 재고가 보충되면 자동으로 판매중이 된다).
 * </ul>
 */
public final class ProductStatusPolicy {

  private ProductStatusPolicy() {}

  public static ProductStatus resolve(ProductStatus desired, long totalStock) {
    if (desired == ProductStatus.HIDDEN) {
      return ProductStatus.HIDDEN;
    }
    return totalStock > 0 ? ProductStatus.ON_SALE : ProductStatus.SOLD_OUT;
  }

  /**
   * 재고 합계. 옵션 상품은 모든 옵션 조합의 합이고, 옵션이 없는 상품은 상품 재고(없으면 0)다.
   *
   * @param productStock 옵션이 없는 상품의 재고(옵션 상품은 null)
   * @param variantStocks 옵션 조합별 재고(옵션이 없으면 빈 목록)
   */
  public static long totalStock(Integer productStock, Collection<Integer> variantStocks) {
    if (!variantStocks.isEmpty()) {
      return variantStocks.stream().mapToLong(Integer::longValue).sum();
    }
    return productStock == null ? 0 : productStock;
  }
}
