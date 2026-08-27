package com.stockpilot.service;

import com.stockpilot.dto.request.SupplierRequest;
import com.stockpilot.dto.response.PagedResponse;
import com.stockpilot.dto.response.SupplierResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SupplierService {
    SupplierResponse createSupplier(SupplierRequest request);
    SupplierResponse getSupplierById(Long id);
    List<SupplierResponse> getAllSuppliers();
    PagedResponse<SupplierResponse> getSuppliers(String search, Boolean active, Pageable pageable);
    SupplierResponse updateSupplier(Long id, SupplierRequest request);
    void deleteSupplier(Long id);
}
