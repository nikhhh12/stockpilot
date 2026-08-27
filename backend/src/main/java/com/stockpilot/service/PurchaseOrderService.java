package com.stockpilot.service;

import com.stockpilot.dto.request.PurchaseOrderCreateRequest;
import com.stockpilot.dto.request.PurchaseOrderReceiveRequest;
import com.stockpilot.dto.response.PagedResponse;
import com.stockpilot.dto.response.PurchaseOrderResponse;
import com.stockpilot.entity.PurchaseOrderStatusEnum;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PurchaseOrderService {
    PurchaseOrderResponse createPurchaseOrder(PurchaseOrderCreateRequest request, Long userId);
    PurchaseOrderResponse getPurchaseOrderById(Long id);
    List<PurchaseOrderResponse> getAllPurchaseOrders();
    PagedResponse<PurchaseOrderResponse> getPurchaseOrders(
            String search, Long supplierId, Long warehouseId, PurchaseOrderStatusEnum status, Pageable pageable
    );
    PurchaseOrderResponse approvePurchaseOrder(Long id, Long userId);
    PurchaseOrderResponse orderPurchaseOrder(Long id, Long userId);
    PurchaseOrderResponse receivePurchaseOrder(Long id, PurchaseOrderReceiveRequest request, Long userId);
    PurchaseOrderResponse cancelPurchaseOrder(Long id, Long userId);
}
