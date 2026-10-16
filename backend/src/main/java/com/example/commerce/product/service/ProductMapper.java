package com.example.commerce.product.service;

import com.example.commerce.product.api.OptionGroupDto;
import com.example.commerce.product.api.ProductImageResponse;
import com.example.commerce.product.api.ProductResponse;
import com.example.commerce.product.api.ProductSummaryResponse;
import com.example.commerce.product.api.VariantDto;
import com.example.commerce.product.domain.Product;
import com.example.commerce.product.domain.ProductImage;
import com.example.commerce.product.domain.ProductOptionValue;
import com.example.commerce.product.domain.ProductStatusPolicy;
import com.example.commerce.product.domain.ProductVariant;
import com.example.commerce.product.domain.VariantCombinationKey;
import org.springframework.stereotype.Component;

/** 엔터티를 응답 DTO로 바꾼다. 트랜잭션 안에서 호출해야 지연 로딩이 동작한다. */
@Component
public class ProductMapper {

  public static final String IMAGE_PATH = "/api/v1/images/";

  public static String imageUrl(String storageKey) {
    return IMAGE_PATH + storageKey;
  }

  public ProductResponse toResponse(Product product) {
    return new ProductResponse(
        product.getId(),
        product.getCode(),
        product.getName(),
        product.getDescription(),
        product.getPrice(),
        product.getCategory().getId(),
        product.getStatus(),
        product.getStockQuantity(),
        product.getOptionGroups().stream()
            .map(
                group ->
                    new OptionGroupDto(
                        group.getName(),
                        group.getValues().stream().map(ProductOptionValue::getValue).toList()))
            .toList(),
        product.getVariants().stream().map(this::toVariantDto).toList(),
        product.getImages().stream().map(this::toImageResponse).toList(),
        product.getVersion(),
        product.getCreatedAt(),
        product.getUpdatedAt());
  }

  public ProductSummaryResponse toSummary(Product product) {
    long totalStock =
        ProductStatusPolicy.totalStock(
            product.getStockQuantity(),
            product.getVariants().stream().map(ProductVariant::getStockQuantity).toList());
    String primaryImageUrl =
        product.getImages().stream()
            .filter(ProductImage::isPrimary)
            .findFirst()
            .map(image -> imageUrl(image.getStorageKey()))
            .orElse(null);
    return new ProductSummaryResponse(
        product.getId(),
        product.getCode(),
        product.getName(),
        product.getPrice(),
        product.getCategory().getId(),
        product.getStatus(),
        totalStock,
        primaryImageUrl,
        product.getCreatedAt());
  }

  public ProductImageResponse toImageResponse(ProductImage image) {
    return new ProductImageResponse(
        image.getId(), imageUrl(image.getStorageKey()), image.isPrimary(), image.getPosition());
  }

  private VariantDto toVariantDto(ProductVariant variant) {
    return new VariantDto(
        VariantCombinationKey.parse(variant.getCombinationKey()), variant.getStockQuantity());
  }
}
