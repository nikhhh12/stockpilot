package com.stockpilot.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderReceiveRequest {

    @NotEmpty(message = "At least one received item is required")
    @Valid
    private List<PurchaseOrderItemReceiveDto> items;
}
