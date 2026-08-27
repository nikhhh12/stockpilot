package com.stockpilot.service.impl;

import com.stockpilot.dto.request.CategoryRequest;
import com.stockpilot.dto.response.CategoryResponse;
import com.stockpilot.dto.response.PagedResponse;
import com.stockpilot.entity.Category;
import com.stockpilot.exception.DuplicateResourceException;
import com.stockpilot.exception.InvalidOperationException;
import com.stockpilot.exception.ResourceNotFoundException;
import com.stockpilot.mapper.CategoryMapper;
import com.stockpilot.repository.CategoryRepository;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName().trim())) {
            throw new DuplicateResourceException("Category with name '" + request.getName() + "' already exists");
        }

        Category category = Category.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .active(request.isActive())
                .build();

        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        long count = productRepository.countByCategoryId(id);
        return categoryMapper.toResponse(category, count);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(cat -> categoryMapper.toResponse(cat, productRepository.countByCategoryId(cat.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CategoryResponse> getCategories(String search, Boolean active, Pageable pageable) {
        Page<Category> page = categoryRepository.findAll((root, query, cb) -> {
            var predicate = cb.conjunction();
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            if (active != null) {
                predicate = cb.and(predicate, cb.equal(root.get("active"), active));
            }
            return predicate;
        }, pageable);

        Page<CategoryResponse> mappedPage = page.map(cat ->
                categoryMapper.toResponse(cat, productRepository.countByCategoryId(cat.getId()))
        );
        return PagedResponse.from(mappedPage);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (!category.getName().equalsIgnoreCase(request.getName().trim()) &&
                categoryRepository.existsByName(request.getName().trim())) {
            throw new DuplicateResourceException("Category with name '" + request.getName() + "' already exists");
        }

        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());
        category.setActive(request.isActive());

        Category saved = categoryRepository.save(category);
        long count = productRepository.countByCategoryId(id);
        return categoryMapper.toResponse(saved, count);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        long count = productRepository.countByCategoryId(id);
        if (count > 0) {
            // Deactivate instead of physical deletion if products depend on it
            category.setActive(false);
            categoryRepository.save(category);
        } else {
            categoryRepository.delete(category);
        }
    }
}
