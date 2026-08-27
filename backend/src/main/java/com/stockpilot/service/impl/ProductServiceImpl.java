package com.stockpilot.service.impl;

import com.stockpilot.dto.request.ProductCreateRequest;
import com.stockpilot.dto.request.ProductUpdateRequest;
import com.stockpilot.dto.response.PagedResponse;
import com.stockpilot.dto.response.ProductResponse;
import com.stockpilot.entity.Category;
import com.stockpilot.entity.Inventory;
import com.stockpilot.entity.Product;
import com.stockpilot.entity.Supplier;
import com.stockpilot.exception.DuplicateResourceException;
import com.stockpilot.exception.ResourceNotFoundException;
import com.stockpilot.mapper.ProductMapper;
import com.stockpilot.repository.CategoryRepository;
import com.stockpilot.repository.InventoryRepository;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.SupplierRepository;
import com.stockpilot.service.ProductService;
import com.stockpilot.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        String sku = request.getSku().trim().toUpperCase();
        if (productRepository.existsBySku(sku)) {
            throw new DuplicateResourceException("Product with SKU '" + sku + "' already exists");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", request.getSupplierId()));

        Product product = Product.builder()
                .sku(sku)
                .name(request.getName().trim())
                .description(request.getDescription())
                .category(category)
                .supplier(supplier)
                .costPrice(request.getCostPrice())
                .sellingPrice(request.getSellingPrice())
                .unit(request.getUnit().trim())
                .reorderLevel(request.getReorderLevel())
                .reorderQuantity(request.getReorderQuantity())
                .active(request.isActive())
                .build();

        Product saved = productRepository.save(product);
        return buildProductResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return buildProductResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "sku", sku));
        return buildProductResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::buildProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getProducts(String search, Long categoryId, Long supplierId, Boolean active, Pageable pageable) {
        Page<Product> page = productRepository.findAll(
                ProductSpecification.filterProducts(search, categoryId, supplierId, active),
                pageable
        );

        Page<ProductResponse> mappedPage = page.map(this::buildProductResponse);
        return PagedResponse.from(mappedPage);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", request.getSupplierId()));

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setCategory(category);
        product.setSupplier(supplier);
        product.setCostPrice(request.getCostPrice());
        product.setSellingPrice(request.getSellingPrice());
        product.setUnit(request.getUnit().trim());
        product.setReorderLevel(request.getReorderLevel());
        product.setReorderQuantity(request.getReorderQuantity());
        product.setActive(request.isActive());

        Product saved = productRepository.save(product);
        return buildProductResponse(saved);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        List<Inventory> inventories = inventoryRepository.findByProductId(id);
        long totalOnHand = inventories.stream().mapToLong(Inventory::getOnHandQuantity).sum();

        if (totalOnHand > 0 || !inventories.isEmpty()) {
            // Deactivate historically referenced product rather than physical deletion
            product.setActive(false);
            productRepository.save(product);
        } else {
            productRepository.delete(product);
        }
    }

    private ProductResponse buildProductResponse(Product product) {
        List<Inventory> inventories = inventoryRepository.findByProductId(product.getId());
        long avail = inventories.stream().mapToLong(i -> i.getAvailableQuantity() != null ? i.getAvailableQuantity() : 0).sum();
        long res = inventories.stream().mapToLong(i -> i.getReservedQuantity() != null ? i.getReservedQuantity() : 0).sum();
        long dam = inventories.stream().mapToLong(i -> i.getDamagedQuantity() != null ? i.getDamagedQuantity() : 0).sum();

        return productMapper.toResponse(product, avail, res, dam);
    }
}
