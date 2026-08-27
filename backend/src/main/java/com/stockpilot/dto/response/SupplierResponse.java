package com.stockpilot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponse {
    private Long id;
    private String companyName;
    private String contactPerson;
    private String email;
    private String phone;
    private String address;
    private String taxIdentifier;
    private Integer defaultLeadTimeDays;
    private boolean active;
    private long productCount;
    private long purchaseOrderCount;
    private BigDecimal totalPurchaseValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
