package com.stockpilot.mapper;

import com.stockpilot.dto.response.InventoryResponse;
import com.stockpilot.entity.Inventory;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public InventoryResponse toResponse(Inventory inventory, String stockoutRisk) {
        if (inventory == null) return null;

        int available = inventory.getAvailableQuantity() != null ? inventory.getAvailableQuantity() : 0;
        int reorderLvl = (inventory.getProduct() != null && inventory.getProduct().getReorderLevel() != null)
                ? inventory.getProduct().getReorderLevel() : 10;

        String stockStatus = "HEALTHY";
        if (available == 0) {
            stockStatus = "OUT_OF_STOCK";
        } else if (available <= reorderLvl) {
            stockStatus = "LOW_STOCK";
        }

        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProduct() != null ? inventory.getProduct().getId() : null)
                .productSku(inventory.getProduct() != null ? inventory.getProduct().getSku() : null)
                .productName(inventory.getProduct() != null ? inventory.getProduct().getName() : null)
                .productUnit(inventory.getProduct() != null ? inventory.getProduct().getUnit() : null)
                .reorderLevel(reorderLvl)
                .warehouseId(inventory.getWarehouse() != null ? inventory.getWarehouse().getId() : null)
                .warehouseName(inventory.getWarehouse() != null ? inventory.getWarehouse().getName() : null)
                .warehouseCode(inventory.getWarehouse() != null ? inventory.getWarehouse().getCode() : null)
                .availableQuantity(available)
                .reservedQuantity(inventory.getReservedQuantity() != null ? inventory.getReservedQuantity() : 0)
                .damagedQuantity(inventory.getDamagedQuantity() != null ? inventory.getDamagedQuantity() : 0)
                .onHandQuantity(inventory.getOnHandQuantity())
                .stockStatus(stockStatus)
                .stockoutRisk(stockoutRisk != null ? stockoutRisk : "LOW")
                .version(inventory.getVersion())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}
