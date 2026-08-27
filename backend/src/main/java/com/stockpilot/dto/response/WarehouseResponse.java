package com.stockpilot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseResponse {
    private Long id;
    private String name;
    private String code;
    private String address;
    private String city;
    private String state;
    private String country;
    private boolean active;
    private long totalAvailableStock;
    private long totalReservedStock;
    private long totalDamagedStock;
    private long totalOnHandStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
