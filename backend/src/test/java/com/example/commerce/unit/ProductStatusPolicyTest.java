package com.example.commerce.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.commerce.product.domain.ProductStatus;
import com.example.commerce.product.domain.ProductStatusPolicy;
import org.junit.jupiter.api.Test;

/** FR-008, FR-009, FR-016: 판매 상태는 운영자가 지정한 값과 재고 합계로 결정된다. */
class ProductStatusPolicyTest {

  @Test
  void 숨김은_재고와_무관하게_유지된다() {
    assertThat(ProductStatusPolicy.resolve(ProductStatus.HIDDEN, 0))
        .isEqualTo(ProductStatus.HIDDEN);
    assertThat(ProductStatusPolicy.resolve(ProductStatus.HIDDEN, 50))
        .isEqualTo(ProductStatus.HIDDEN);
  }

  @Test
  void 판매중으로_지정해도_재고가_0이면_품절이다() {
    assertThat(ProductStatusPolicy.resolve(ProductStatus.ON_SALE, 0))
        .isEqualTo(ProductStatus.SOLD_OUT);
  }

  @Test
  void 판매중으로_지정하고_재고가_있으면_판매중이다() {
    assertThat(ProductStatusPolicy.resolve(ProductStatus.ON_SALE, 1))
        .isEqualTo(ProductStatus.ON_SALE);
  }

  @Test
  void 품절_상품은_재고가_보충되면_판매중으로_자동_복귀한다() {
    assertThat(ProductStatusPolicy.resolve(ProductStatus.SOLD_OUT, 3))
        .isEqualTo(ProductStatus.ON_SALE);
  }

  @Test
  void 품절_상품은_재고가_0이면_품절이_유지된다() {
    assertThat(ProductStatusPolicy.resolve(ProductStatus.SOLD_OUT, 0))
        .isEqualTo(ProductStatus.SOLD_OUT);
  }

  @Test
  void 옵션_상품의_재고_합계는_모든_옵션_조합의_합이다() {
    assertThat(ProductStatusPolicy.totalStock(null, java.util.List.of(0, 0, 0))).isZero();
    assertThat(ProductStatusPolicy.totalStock(null, java.util.List.of(0, 2, 5))).isEqualTo(7);
  }

  @Test
  void 일부_옵션_조합만_재고가_0이어도_상품은_판매중이다() {
    long total = ProductStatusPolicy.totalStock(null, java.util.List.of(0, 4));
    assertThat(ProductStatusPolicy.resolve(ProductStatus.ON_SALE, total))
        .isEqualTo(ProductStatus.ON_SALE);
  }

  @Test
  void 옵션이_없는_상품의_재고_합계는_상품_재고이다() {
    assertThat(ProductStatusPolicy.totalStock(6, java.util.List.of())).isEqualTo(6);
    assertThat(ProductStatusPolicy.totalStock(null, java.util.List.of())).isZero();
  }
}
