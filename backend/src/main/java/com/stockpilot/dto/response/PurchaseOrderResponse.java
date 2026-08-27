package com.stockpilot.dto.response;

import com.stockpilot.entity.PurchaseOrderStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderResponse {
    private Long id;
    private String poNumber;
    private Long supplierId;
    private String supplierCompanyName;
    private Long warehouseId;
    private String warehouseName;
    private PurchaseOrderStatusEnum status;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private LocalDate receivedDate;
    private BigDecimal totalAmount;
    private String notes;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PurchaseOrderItemResponse> items;
}
