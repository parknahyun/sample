package com.example.commerce.storefront;

import com.example.commerce.common.NotFoundException;
import com.example.commerce.common.ValidationFailedException;
import com.example.commerce.product.api.OptionGroupDto;
import com.example.commerce.product.domain.Product;
import com.example.commerce.product.domain.ProductOptionValue;
import com.example.commerce.product.domain.ProductStatus;
import com.example.commerce.product.domain.VariantCombinationKey;
import com.example.commerce.product.repository.ProductRepository;
import com.example.commerce.product.service.ProductMapper;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 고객 노출용 조회(FR-018). 판매중·품절 상품만 노출하고 숨김·삭제된 상품은 제외한다. 별도 캐시 없이 DB를 직접 조회하므로 운영자의 수정이 즉시
 * 반영된다(SC-003).
 */
@Service
public class StorefrontQueryService {

  private static final int MAX_SIZE = 100;
  private static final List<ProductStatus> VISIBLE =
      List.of(ProductStatus.ON_SALE, ProductStatus.SOLD_OUT);

  private final ProductRepository products;
  private final ProductMapper mapper;

  StorefrontQueryService(ProductRepository products, ProductMapper mapper) {
    this.products = products;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public StorefrontProductPageResponse list(Long categoryId, int page, int size) {
    if (page < 0) {
      throw new ValidationFailedException("page", "페이지 번호는 0 이상이어야 합니다.");
    }
    if (size < 1 || size > MAX_SIZE) {
      throw new ValidationFailedException("size", "페이지 크기는 1 이상 " + MAX_SIZE + " 이하여야 합니다.");
    }
    Specification<Product> spec = (root, query, cb) -> root.get("status").in(VISIBLE);
    if (categoryId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
    }
    Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    return StorefrontProductPageResponse.of(
        products.findAll(spec, PageRequest.of(page, size, sort)).map(this::toResponse));
  }

  @Transactional(readOnly = true)
  public StorefrontProductResponse get(long productId) {
    return products
        .findById(productId)
        .filter(product -> VISIBLE.contains(product.getStatus()))
        .map(this::toResponse)
        .orElseThrow(() -> new NotFoundException("존재하지 않는 상품입니다."));
  }

  private StorefrontProductResponse toResponse(Product product) {
    return new StorefrontProductResponse(
        product.getId(),
        product.getName(),
        product.getDescription(),
        product.getPrice(),
        product.getCategory().getId(),
        product.getStatus() == ProductStatus.SOLD_OUT,
        product.getOptionGroups().stream()
            .map(
                group ->
                    new OptionGroupDto(
                        group.getName(),
                        group.getValues().stream().map(ProductOptionValue::getValue).toList()))
            .toList(),
        product.getVariants().stream()
            .map(
                variant ->
                    new StorefrontProductResponse.StorefrontVariantResponse(
                        VariantCombinationKey.parse(variant.getCombinationKey()),
                        variant.getStockQuantity() == 0))
            .toList(),
        product.getImages().stream().map(mapper::toImageResponse).toList());
  }
}
