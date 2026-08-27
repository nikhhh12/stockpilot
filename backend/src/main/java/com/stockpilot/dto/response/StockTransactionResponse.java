package com.stockpilot.dto.response;

import com.stockpilot.entity.TransactionTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransactionResponse {
    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private Long warehouseId;
    private String warehouseName;
    private TransactionTypeEnum transactionType;
    private Integer quantity;
    private Integer previousAvailableQuantity;
    private Integer newAvailableQuantity;
    private Integer previousReservedQuantity;
    private Integer newReservedQuantity;
    private Integer previousDamagedQuantity;
    private Integer newDamagedQuantity;
    private String reason;
    private String referenceNumber;
    private String notes;
    private Long performedById;
    private String performedByName;
    private LocalDateTime createdAt;
}
