package com.stockpilot.mapper;

import com.stockpilot.dto.response.StockTransactionResponse;
import com.stockpilot.entity.StockTransaction;
import org.springframework.stereotype.Component;

@Component
public class StockTransactionMapper {

    public StockTransactionResponse toResponse(StockTransaction st) {
        if (st == null) return null;
        return StockTransactionResponse.builder()
                .id(st.getId())
                .productId(st.getProduct() != null ? st.getProduct().getId() : null)
                .productSku(st.getProduct() != null ? st.getProduct().getSku() : null)
                .productName(st.getProduct() != null ? st.getProduct().getName() : null)
                .warehouseId(st.getWarehouse() != null ? st.getWarehouse().getId() : null)
                .warehouseName(st.getWarehouse() != null ? st.getWarehouse().getName() : null)
                .transactionType(st.getTransactionType())
                .quantity(st.getQuantity())
                .previousAvailableQuantity(st.getPreviousAvailableQuantity())
                .newAvailableQuantity(st.getNewAvailableQuantity())
                .previousReservedQuantity(st.getPreviousReservedQuantity())
                .newReservedQuantity(st.getNewReservedQuantity())
                .previousDamagedQuantity(st.getPreviousDamagedQuantity())
                .newDamagedQuantity(st.getNewDamagedQuantity())
                .reason(st.getReason())
                .referenceNumber(st.getReferenceNumber())
                .notes(st.getNotes())
                .performedById(st.getPerformedBy() != null ? st.getPerformedBy().getId() : null)
                .performedByName(st.getPerformedBy() != null ? st.getPerformedBy().getName() : null)
                .createdAt(st.getCreatedAt())
                .build();
    }
}
