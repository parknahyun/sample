package com.example.commerce.category;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 카테고리 조회(운영자 전용). 카테고리 관리는 별도 기능이다. */
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

  public record CategoryResponse(Long id, String name) {}

  private final CategoryRepository categories;

  CategoryController(CategoryRepository categories) {
    this.categories = categories;
  }

  @GetMapping
  public List<CategoryResponse> list() {
    return categories.findAllByOrderByIdAsc().stream()
        .map(category -> new CategoryResponse(category.getId(), category.getName()))
        .toList();
  }
}
