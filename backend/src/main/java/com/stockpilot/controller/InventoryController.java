package com.stockpilot.controller;

import com.stockpilot.dto.request.*;
import com.stockpilot.dto.response.ApiResponse;
import com.stockpilot.dto.response.InventoryResponse;
import com.stockpilot.dto.response.PagedResponse;
import com.stockpilot.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<InventoryResponse>>> getInventories(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PagedResponse<InventoryResponse> response = inventoryService.getInventories(search, warehouseId, categoryId, stockStatus, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryById(@PathVariable Long id) {
        InventoryResponse response = inventoryService.getInventoryById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getInventoryByProduct(@PathVariable Long productId) {
        List<InventoryResponse> list = inventoryService.getInventoryByProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getInventoryByWarehouse(@PathVariable Long warehouseId) {
        List<InventoryResponse> list = inventoryService.getInventoryByWarehouse(warehouseId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getLowStockInventories() {
        List<InventoryResponse> list = inventoryService.getLowStockInventories();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/out-of-stock")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getOutOfStockInventories() {
        List<InventoryResponse> list = inventoryService.getOutOfStockInventories();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/stock-in")
    public ResponseEntity<ApiResponse<InventoryResponse>> stockIn(@Valid @RequestBody StockInRequest request) {
        InventoryResponse response = inventoryService.stockIn(request, null);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock IN completed successfully"));
    }

    @PostMapping("/stock-out")
    public ResponseEntity<ApiResponse<InventoryResponse>> stockOut(@Valid @RequestBody StockOutRequest request) {
        InventoryResponse response = inventoryService.stockOut(request, null);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock OUT completed successfully"));
    }

    @PostMapping("/adjust")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjustStock(@Valid @RequestBody StockAdjustmentRequest request) {
        InventoryResponse response = inventoryService.adjustStock(request, null);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock adjustment completed successfully"));
    }

    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<InventoryResponse>> reserveStock(@Valid @RequestBody StockReserveRequest request) {
        InventoryResponse response = inventoryService.reserveStock(request, null);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock reservation completed successfully"));
    }

    @PostMapping("/release")
    public ResponseEntity<ApiResponse<InventoryResponse>> releaseStock(@Valid @RequestBody StockReleaseRequest request) {
        InventoryResponse response = inventoryService.releaseStock(request, null);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock reservation release completed successfully"));
    }

    @PostMapping("/damage")
    public ResponseEntity<ApiResponse<InventoryResponse>> damageStock(@Valid @RequestBody StockDamageRequest request) {
        InventoryResponse response = inventoryService.damageStock(request, null);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock marked as damaged successfully"));
    }

    @PostMapping("/return")
    public ResponseEntity<ApiResponse<InventoryResponse>> returnStock(@Valid @RequestBody StockReturnRequest request) {
        InventoryResponse response = inventoryService.returnStock(request, null);
        return ResponseEntity.ok(ApiResponse.success(response, "Returned stock restored to available inventory"));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> transferStock(@Valid @RequestBody StockTransferRequest request) {
        List<InventoryResponse> response = inventoryService.transferStock(request, null);
        return ResponseEntity.ok(ApiResponse.success(response, "Warehouse-to-warehouse stock transfer completed successfully"));
    }
}
