package com.stockpilot.service;

import com.stockpilot.dto.request.WarehouseRequest;
import com.stockpilot.dto.response.PagedResponse;
import com.stockpilot.dto.response.WarehouseResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WarehouseService {
    WarehouseResponse createWarehouse(WarehouseRequest request);
    WarehouseResponse getWarehouseById(Long id);
    List<WarehouseResponse> getAllWarehouses();
    PagedResponse<WarehouseResponse> getWarehouses(String search, Boolean active, Pageable pageable);
    WarehouseResponse updateWarehouse(Long id, WarehouseRequest request);
    void deleteWarehouse(Long id);
}
