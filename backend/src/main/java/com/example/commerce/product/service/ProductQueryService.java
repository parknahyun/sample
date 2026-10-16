package com.example.commerce.product.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.example.commerce.common.NotFoundException;
import com.example.commerce.common.ValidationFailedException;
import com.example.commerce.product.api.ProductPageResponse;
import com.example.commerce.product.api.ProductResponse;
import com.example.commerce.product.domain.Product;
import com.example.commerce.product.domain.ProductStatus;
import com.example.commerce.product.repository.ProductRepository;
import com.example.commerce.product.repository.ProductSpecifications;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 운영자용 상품 조회·검색(FR-006, FR-010, FR-011). 삭제된 상품은 조회되지 않는다. */
@Service
public class ProductQueryService {

  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;
  public static final String DEFAULT_SORT = "createdAt,desc";

  private static final Logger log = LoggerFactory.getLogger(ProductQueryService.class);

  /** 허용하는 정렬 기준. 같은 값이 있을 때는 최신 등록 순으로 안정화한다. */
  private static final Map<String, Sort> SORTS =
      Map.of(
          "createdAt,desc", Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")),
          "createdAt,asc", Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")),
          "price,asc",
              Sort.by(Sort.Order.asc("price"), Sort.Order.desc("createdAt"), Sort.Order.desc("id")),
          "price,desc",
              Sort.by(
                  Sort.Order.desc("price"), Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

  private final ProductRepository products;
  private final ProductMapper mapper;

  ProductQueryService(ProductRepository products, ProductMapper mapper) {
    this.products = products;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public ProductResponse get(long productId) {
    return products
        .findById(productId)
        .map(mapper::toResponse)
        .orElseThrow(() -> new NotFoundException("존재하지 않는 상품입니다."));
  }

  /**
   * 상품 목록을 검색한다.
   *
   * @param q 상품명 부분 일치 검색어(없으면 전체)
   * @param categoryId 카테고리 필터
   * @param status 판매 상태 필터
   * @param sort {@code createdAt,desc|asc} 또는 {@code price,desc|asc}(없으면 등록일 내림차순)
   */
  @Transactional(readOnly = true)
  public ProductPageResponse list(
      String q, Long categoryId, ProductStatus status, String sort, int page, int size) {
    if (page < 0) {
      throw new ValidationFailedException("page", "페이지 번호는 0 이상이어야 합니다.");
    }
    if (size < 1 || size > MAX_SIZE) {
      throw new ValidationFailedException("size", "페이지 크기는 1 이상 " + MAX_SIZE + " 이하여야 합니다.");
    }
    String sortKey = sort == null || sort.isBlank() ? DEFAULT_SORT : sort;
    Sort resolved = SORTS.get(sortKey);
    if (resolved == null) {
      throw new ValidationFailedException(
          "sort",
          "허용되지 않는 정렬 기준입니다. createdAt,desc | createdAt,asc | price,desc | price,asc 중에서 고르세요.");
    }

    long start = System.nanoTime();
    Page<Product> result =
        products.findAll(
            ProductSpecifications.search(q, categoryId, status),
            PageRequest.of(page, size, resolved));
    ProductPageResponse response = ProductPageResponse.of(result.map(mapper::toSummary));
    log.info(
        "상품 검색",
        kv("event", "product_search"),
        kv("q", q),
        kv("categoryId", categoryId),
        kv("status", status),
        kv("sort", sortKey),
        kv("page", page),
        kv("size", size),
        kv("totalElements", result.getTotalElements()),
        kv("elapsedMs", (System.nanoTime() - start) / 1_000_000));
    return response;
  }
}
