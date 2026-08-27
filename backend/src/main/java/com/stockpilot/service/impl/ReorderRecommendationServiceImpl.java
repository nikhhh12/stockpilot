package com.stockpilot.service.impl;

import com.stockpilot.dto.response.ReorderRecommendationResponse;
import com.stockpilot.entity.*;
import com.stockpilot.exception.ResourceNotFoundException;
import com.stockpilot.repository.*;
import com.stockpilot.service.ReorderRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReorderRecommendationServiceImpl implements ReorderRecommendationService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final WarehouseRepository warehouseRepository;

    private static final int DEFAULT_LEAD_TIME_DAYS = 7;

    @Override
    @Transactional(readOnly = true)
    public List<ReorderRecommendationResponse> getReorderRecommendations() {
        List<Product> activeProducts = productRepository.findAll().stream()
                .filter(Product::isActive)
                .collect(Collectors.toList());

        List<ReorderRecommendationResponse> recommendations = new ArrayList<>();
        for (Product product : activeProducts) {
            ReorderRecommendationResponse rec = calculateRecommendation(product);
            if (rec != null) {
                recommendations.add(rec);
            }
        }

        // Sort by urgency: CRITICAL first, then HIGH, then MEDIUM
        return recommendations.stream()
                .sorted(Comparator.comparingInt(r -> getUrgencyPriority(r.getUrgency())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReorderRecommendationResponse getProductReorderRecommendation(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        ReorderRecommendationResponse rec = calculateRecommendation(product);
        if (rec == null) {
            // Return baseline healthy recommendation
            return buildHealthyRecommendation(product);
        }
        return rec;
    }

    private ReorderRecommendationResponse calculateRecommendation(Product product) {
        List<Inventory> inventories = inventoryRepository.findByProductId(product.getId());
        int totalAvailable = inventories.stream()
                .mapToInt(i -> i.getAvailableQuantity() != null ? i.getAvailableQuantity() : 0)
                .sum();
        int totalReserved = inventories.stream()
                .mapToInt(i -> i.getReservedQuantity() != null ? i.getReservedQuantity() : 0)
                .sum();

        // Calculate pipeline on-order quantity from active POs
        int pipelineOnOrder = 0;
        List<PurchaseOrder> activePos = purchaseOrderRepository.findAll().stream()
                .filter(po -> po.getStatus() == PurchaseOrderStatusEnum.PENDING ||
                        po.getStatus() == PurchaseOrderStatusEnum.APPROVED ||
                        po.getStatus() == PurchaseOrderStatusEnum.ORDERED ||
                        po.getStatus() == PurchaseOrderStatusEnum.PARTIALLY_RECEIVED)
                .collect(Collectors.toList());

        for (PurchaseOrder po : activePos) {
            if (po.getItems() != null) {
                for (PurchaseOrderItem item : po.getItems()) {
                    if (item.getProduct().getId().equals(product.getId())) {
                        pipelineOnOrder += (item.getOrderedQuantity() - item.getReceivedQuantity());
                    }
                }
            }
        }

        // Average daily demand
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<StockTransaction> consumptionTx = stockTransactionRepository.findAll().stream()
                .filter(t -> t.getProduct().getId().equals(product.getId()) &&
                        (t.getTransactionType() == TransactionTypeEnum.STOCK_OUT || t.getTransactionType() == TransactionTypeEnum.RESERVE) &&
                        t.getCreatedAt().isAfter(thirtyDaysAgo))
                .collect(Collectors.toList());

        int totalConsumed = consumptionTx.stream().mapToInt(StockTransaction::getQuantity).sum();
        double avgDailyDemand = totalConsumed > 0 ? (double) totalConsumed / 30.0 : 1.0;
        avgDailyDemand = Math.round(avgDailyDemand * 100.0) / 100.0;

        int leadTimeDays = (product.getSupplier() != null && product.getSupplier().getDefaultLeadTimeDays() != null && product.getSupplier().getDefaultLeadTimeDays() > 0)
                ? product.getSupplier().getDefaultLeadTimeDays()
                : DEFAULT_LEAD_TIME_DAYS;

        double safetyFactor = 1.5;
        int safetyStock = (int) Math.max(product.getReorderLevel() != null ? product.getReorderLevel() : 10, Math.round(avgDailyDemand * safetyFactor));
        double leadTimeDemand = avgDailyDemand * leadTimeDays;
        int inventoryPosition = totalAvailable + totalReserved + pipelineOnOrder;
        double reorderNeed = leadTimeDemand + safetyStock - inventoryPosition;

        int reorderQty = Math.max(0, (int) Math.ceil(reorderNeed));
        if (totalAvailable <= product.getReorderLevel() && reorderQty == 0) {
            reorderQty = product.getReorderQuantity() != null ? product.getReorderQuantity() : 20;
        }

        boolean needsReorder = totalAvailable <= product.getReorderLevel() || reorderQty > 0;
        if (!needsReorder) {
            return null;
        }

        String urgency;
        String reason;
        if (totalAvailable == 0) {
            urgency = "CRITICAL";
            reason = String.format("Zero physical available inventory remaining. Lead time: %d days.", leadTimeDays);
        } else if (totalAvailable <= (safetyStock / 2)) {
            urgency = "CRITICAL";
            reason = String.format("Available stock (%d) has breached 50%% of safety buffer (%d). Lead time: %d days.", totalAvailable, safetyStock, leadTimeDays);
        } else if (totalAvailable <= product.getReorderLevel()) {
            urgency = "HIGH";
            reason = String.format("Available stock (%d) is at or below reorder trigger level (%d). Inventory Position: %d.", totalAvailable, product.getReorderLevel(), inventoryPosition);
        } else {
            urgency = "MEDIUM";
            reason = String.format("Projected demand over supplier lead time (%.1f units over %d days) exceeds inventory position (%d).", leadTimeDemand, leadTimeDays, inventoryPosition);
        }

        // Default warehouse for PO target
        Warehouse defaultWarehouse = inventories.isEmpty() ?
                warehouseRepository.findAll().stream().findFirst().orElse(null) :
                inventories.get(0).getWarehouse();

        BigDecimal unitCost = product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO;
        BigDecimal estimatedCost = unitCost.multiply(BigDecimal.valueOf(reorderQty));

        return ReorderRecommendationResponse.builder()
                .productId(product.getId())
                .productSku(product.getSku())
                .productName(product.getName())
                .preferredSupplierId(product.getSupplier() != null ? product.getSupplier().getId() : null)
                .preferredSupplierName(product.getSupplier() != null ? product.getSupplier().getCompanyName() : "Unassigned")
                .warehouseId(defaultWarehouse != null ? defaultWarehouse.getId() : 1L)
                .warehouseName(defaultWarehouse != null ? defaultWarehouse.getName() : "Main Hub")
                .availableStock(totalAvailable)
                .reservedStock(totalReserved)
                .pipelineOnOrderQuantity(pipelineOnOrder)
                .averageDailyDemand(avgDailyDemand)
                .safetyStock(safetyStock)
                .recommendedReorderQuantity(reorderQty)
                .estimatedCost(estimatedCost)
                .urgency(urgency)
                .reason(reason)
                .build();
    }

    private ReorderRecommendationResponse buildHealthyRecommendation(Product product) {
        List<Inventory> inventories = inventoryRepository.findByProductId(product.getId());
        int totalAvailable = inventories.stream().mapToInt(i -> i.getAvailableQuantity() != null ? i.getAvailableQuantity() : 0).sum();
        int totalReserved = inventories.stream().mapToInt(i -> i.getReservedQuantity() != null ? i.getReservedQuantity() : 0).sum();

        return ReorderRecommendationResponse.builder()
                .productId(product.getId())
                .productSku(product.getSku())
                .productName(product.getName())
                .preferredSupplierId(product.getSupplier() != null ? product.getSupplier().getId() : null)
                .preferredSupplierName(product.getSupplier() != null ? product.getSupplier().getCompanyName() : "Unassigned")
                .warehouseId(inventories.isEmpty() ? 1L : inventories.get(0).getWarehouse().getId())
                .warehouseName(inventories.isEmpty() ? "Main Hub" : inventories.get(0).getWarehouse().getName())
                .availableStock(totalAvailable)
                .reservedStock(totalReserved)
                .pipelineOnOrderQuantity(0)
                .averageDailyDemand(1.0)
                .safetyStock(product.getReorderLevel())
                .recommendedReorderQuantity(0)
                .estimatedCost(BigDecimal.ZERO)
                .urgency("LOW")
                .reason("Inventory level is currently optimal.")
                .build();
    }

    private int getUrgencyPriority(String urgency) {
        if ("CRITICAL".equalsIgnoreCase(urgency)) return 1;
        if ("HIGH".equalsIgnoreCase(urgency)) return 2;
        if ("MEDIUM".equalsIgnoreCase(urgency)) return 3;
        return 4;
    }
}
