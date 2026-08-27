package com.stockpilot.service;

import com.stockpilot.dto.request.ProductCreateRequest;
import com.stockpilot.dto.request.ProductUpdateRequest;
import com.stockpilot.dto.response.PagedResponse;
import com.stockpilot.dto.response.ProductResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductCreateRequest request);
    ProductResponse getProductById(Long id);
    ProductResponse getProductBySku(String sku);
    List<ProductResponse> getAllProducts();
    PagedResponse<ProductResponse> getProducts(String search, Long categoryId, Long supplierId, Boolean active, Pageable pageable);
    ProductResponse updateProduct(Long id, ProductUpdateRequest request);
    void deleteProduct(Long id);
}
