package com.stockpilot.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransferRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Source warehouse ID is required")
    private Long sourceWarehouseId;

    @NotNull(message = "Destination warehouse ID is required")
    private Long destinationWarehouseId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Transfer quantity must be greater than 0")
    private Integer quantity;

    private String referenceNumber;
    private String notes;
}
