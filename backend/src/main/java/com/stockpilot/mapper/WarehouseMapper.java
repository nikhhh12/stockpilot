package com.stockpilot.mapper;

import com.stockpilot.dto.response.WarehouseResponse;
import com.stockpilot.entity.Warehouse;
import org.springframework.stereotype.Component;

@Component
public class WarehouseMapper {

    public WarehouseResponse toResponse(Warehouse warehouse, long avail, long res, long dam) {
        if (warehouse == null) return null;
        return WarehouseResponse.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .code(warehouse.getCode())
                .address(warehouse.getAddress())
                .city(warehouse.getCity())
                .state(warehouse.getState())
                .country(warehouse.getCountry())
                .active(warehouse.isActive())
                .totalAvailableStock(avail)
                .totalReservedStock(res)
                .totalDamagedStock(dam)
                .totalOnHandStock(avail + res + dam)
                .createdAt(warehouse.getCreatedAt())
                .updatedAt(warehouse.getUpdatedAt())
                .build();
    }
}
