package com.stockpilot.mapper;

import com.stockpilot.dto.response.SupplierResponse;
import com.stockpilot.entity.Supplier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SupplierMapper {

    public SupplierResponse toResponse(Supplier supplier, long productCount, long poCount, BigDecimal totalValue) {
        if (supplier == null) return null;
        return SupplierResponse.builder()
                .id(supplier.getId())
                .companyName(supplier.getCompanyName())
                .contactPerson(supplier.getContactPerson())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .address(supplier.getAddress())
                .taxIdentifier(supplier.getTaxIdentifier())
                .defaultLeadTimeDays(supplier.getDefaultLeadTimeDays())
                .active(supplier.isActive())
                .productCount(productCount)
                .purchaseOrderCount(poCount)
                .totalPurchaseValue(totalValue != null ? totalValue : BigDecimal.ZERO)
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
}
