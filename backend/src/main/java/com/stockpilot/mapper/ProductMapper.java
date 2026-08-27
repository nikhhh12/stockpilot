package com.stockpilot.mapper;

import com.stockpilot.dto.response.ProductResponse;
import com.stockpilot.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product, long available, long reserved, long damaged) {
        if (product == null) return null;
        long onHand = available + reserved + damaged;
        
        String stockStatus = "HEALTHY";
        if (available == 0) {
            stockStatus = "OUT_OF_STOCK";
        } else if (available <= product.getReorderLevel()) {
            stockStatus = "LOW_STOCK";
        }

        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .supplierId(product.getSupplier() != null ? product.getSupplier().getId() : null)
                .supplierName(product.getSupplier() != null ? product.getSupplier().getCompanyName() : null)
                .costPrice(product.getCostPrice())
                .sellingPrice(product.getSellingPrice())
                .unit(product.getUnit())
                .reorderLevel(product.getReorderLevel())
                .reorderQuantity(product.getReorderQuantity())
                .active(product.isActive())
                .totalAvailableStock(available)
                .totalReservedStock(reserved)
                .totalDamagedStock(damaged)
                .totalOnHandStock(onHand)
                .stockStatus(stockStatus)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
