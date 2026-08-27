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
public class PurchaseOrderItemReceiveDto {

    @NotNull(message = "Purchase order item ID is required")
    private Long itemId;

    @NotNull(message = "Received quantity delta is required")
    @Min(value = 1, message = "Received quantity delta must be at least 1")
    private Integer receivedQuantityDelta;
}
