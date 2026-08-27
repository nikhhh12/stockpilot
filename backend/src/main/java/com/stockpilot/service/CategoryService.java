package com.stockpilot.service;

import com.stockpilot.dto.request.CategoryRequest;
import com.stockpilot.dto.response.CategoryResponse;
import com.stockpilot.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse getCategoryById(Long id);
    List<CategoryResponse> getAllCategories();
    PagedResponse<CategoryResponse> getCategories(String search, Boolean active, Pageable pageable);
    CategoryResponse updateCategory(Long id, CategoryRequest request);
    void deleteCategory(Long id);
}
