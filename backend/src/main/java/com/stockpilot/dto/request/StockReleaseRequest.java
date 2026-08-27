package com.stockpilot.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReleaseRequest {

    @NotNull(message = "Reservation ID is required")
    private Long reservationId;

    private Integer quantity;
    private String notes;
}
