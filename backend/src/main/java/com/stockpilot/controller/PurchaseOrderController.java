package com.stockpilot.controller;

import com.stockpilot.dto.request.PurchaseOrderCreateRequest;
import com.stockpilot.dto.request.PurchaseOrderReceiveRequest;
import com.stockpilot.dto.response.ApiResponse;
import com.stockpilot.dto.response.PagedResponse;
import com.stockpilot.dto.response.PurchaseOrderResponse;
import com.stockpilot.entity.PurchaseOrderStatusEnum;
import com.stockpilot.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createPurchaseOrder(@Valid @RequestBody PurchaseOrderCreateRequest request) {
        PurchaseOrderResponse response = purchaseOrderService.createPurchaseOrder(request, null);
        return new ResponseEntity<>(ApiResponse.success(response, "Purchase Order created successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> getPurchaseOrderById(@PathVariable Long id) {
        PurchaseOrderResponse response = purchaseOrderService.getPurchaseOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> getAllPurchaseOrders() {
        List<PurchaseOrderResponse> list = purchaseOrderService.getAllPurchaseOrders();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PurchaseOrderResponse>>> getPurchaseOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) PurchaseOrderStatusEnum status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PagedResponse<PurchaseOrderResponse> response = purchaseOrderService.getPurchaseOrders(search, supplierId, warehouseId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/approve")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> approvePurchaseOrder(@PathVariable Long id) {
        PurchaseOrderResponse response = purchaseOrderService.approvePurchaseOrder(id, null);
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase Order APPROVED successfully"));
    }

    @PostMapping("/{id}/order")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> orderPurchaseOrder(@PathVariable Long id) {
        PurchaseOrderResponse response = purchaseOrderService.orderPurchaseOrder(id, null);
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase Order status updated to ORDERED"));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> receivePurchaseOrder(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderReceiveRequest request
    ) {
        PurchaseOrderResponse response = purchaseOrderService.receivePurchaseOrder(id, request, null);
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase Order items received and inventory updated"));
    }

    @PostMapping("/{id}/cancel")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> cancelPurchaseOrder(@PathVariable Long id) {
        PurchaseOrderResponse response = purchaseOrderService.cancelPurchaseOrder(id, null);
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase Order CANCELLED"));
    }
}
