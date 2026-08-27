package com.stockpilot.mapper;

import com.stockpilot.dto.response.PurchaseOrderItemResponse;
import com.stockpilot.dto.response.PurchaseOrderResponse;
import com.stockpilot.entity.PurchaseOrder;
import com.stockpilot.entity.PurchaseOrderItem;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PurchaseOrderMapper {

    public PurchaseOrderResponse toResponse(PurchaseOrder po) {
        if (po == null) return null;

        List<PurchaseOrderItemResponse> itemResponses = po.getItems() != null ?
                po.getItems().stream().map(this::toItemResponse).collect(Collectors.toList()) :
                Collections.emptyList();

        return PurchaseOrderResponse.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .supplierId(po.getSupplier() != null ? po.getSupplier().getId() : null)
                .supplierCompanyName(po.getSupplier() != null ? po.getSupplier().getCompanyName() : null)
                .warehouseId(po.getWarehouse() != null ? po.getWarehouse().getId() : null)
                .warehouseName(po.getWarehouse() != null ? po.getWarehouse().getName() : null)
                .status(po.getStatus())
                .orderDate(po.getOrderDate())
                .expectedDeliveryDate(po.getExpectedDeliveryDate())
                .receivedDate(po.getReceivedDate())
                .totalAmount(po.getTotalAmount())
                .notes(po.getNotes())
                .createdById(po.getCreatedBy() != null ? po.getCreatedBy().getId() : null)
                .createdByName(po.getCreatedBy() != null ? po.getCreatedBy().getName() : null)
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .items(itemResponses)
                .build();
    }

    public PurchaseOrderItemResponse toItemResponse(PurchaseOrderItem item) {
        if (item == null) return null;
        return PurchaseOrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productSku(item.getProduct() != null ? item.getProduct().getSku() : null)
                .productName(item.getProduct() != null ? item.getProduct().getName() : null)
                .productUnit(item.getProduct() != null ? item.getProduct().getUnit() : null)
                .orderedQuantity(item.getOrderedQuantity())
                .receivedQuantity(item.getReceivedQuantity())
                .unitCost(item.getUnitCost())
                .totalCost(item.getTotalCost())
                .build();
    }
}
