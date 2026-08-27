package com.stockpilot.service.impl;

import com.stockpilot.dto.response.DashboardSummaryResponse;
import com.stockpilot.entity.*;
import com.stockpilot.repository.*;
import com.stockpilot.service.AnomalyDetectionService;
import com.stockpilot.service.DashboardService;
import com.stockpilot.service.StockoutRiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final CategoryRepository categoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockoutRiskService stockoutRiskService;
    private final AnomalyDetectionService anomalyDetectionService;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        List<Product> products = productRepository.findAll();
        List<Inventory> inventories = inventoryRepository.findAll();

        long totalProducts = products.stream().filter(Product::isActive).count();
        long totalInventoryUnits = inventories.stream().mapToLong(Inventory::getOnHandQuantity).sum();

        BigDecimal totalInventoryValue = BigDecimal.ZERO;
        for (Inventory inv : inventories) {
            BigDecimal cost = inv.getProduct().getCostPrice() != null ? inv.getProduct().getCostPrice() : BigDecimal.ZERO;
            totalInventoryValue = totalInventoryValue.add(cost.multiply(BigDecimal.valueOf(inv.getOnHandQuantity())));
        }

        long lowStockCount = inventories.stream()
                .filter(inv -> inv.getAvailableQuantity() > 0 && inv.getAvailableQuantity() <= inv.getProduct().getReorderLevel())
                .count();

        long outOfStockCount = inventories.stream()
                .filter(inv -> inv.getAvailableQuantity() == 0)
                .count();

        long pendingPosCount = purchaseOrderRepository.findAll().stream()
                .filter(po -> po.getStatus() == PurchaseOrderStatusEnum.PENDING || po.getStatus() == PurchaseOrderStatusEnum.APPROVED)
                .count();

        long criticalRiskCount = stockoutRiskService.getCriticalStockoutRisks().size();
        long anomalyCount = anomalyDetectionService.detectTransactionAnomalies().size();

        return DashboardSummaryResponse.builder()
                .totalProducts(totalProducts)
                .totalInventoryUnits(totalInventoryUnits)
                .totalInventoryValue(totalInventoryValue)
                .lowStockCount(lowStockCount)
                .outOfStockCount(outOfStockCount)
                .pendingPurchaseOrdersCount(pendingPosCount)
                .criticalRiskCount(criticalRiskCount)
                .anomalyCount(anomalyCount)
                .inventoryValueByCategory(getInventoryByCategoryChart())
                .inventoryByWarehouse(getInventoryByWarehouseChart())
                .recentStockMovement(getStockMovementChart())
                .topMovingProducts(getTopMovingProductsChart())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardSummaryResponse.StockMovementDto> getStockMovementChart() {
        LocalDateTime fourteenDaysAgo = LocalDateTime.now().minusDays(14);
        List<StockTransaction> transactions = stockTransactionRepository.findAll().stream()
                .filter(t -> t.getCreatedAt().isAfter(fourteenDaysAgo))
                .collect(Collectors.toList());

        Map<LocalDate, long[]> movementMap = new LinkedHashMap<>();
        LocalDate start = LocalDate.now().minusDays(13);
        for (int i = 0; i < 14; i++) {
            movementMap.put(start.plusDays(i), new long[]{0, 0}); // [in, out]
        }

        for (StockTransaction tx : transactions) {
            LocalDate date = tx.getCreatedAt().toLocalDate();
            if (movementMap.containsKey(date)) {
                long[] counts = movementMap.get(date);
                if (tx.getTransactionType() == TransactionTypeEnum.STOCK_IN || tx.getTransactionType() == TransactionTypeEnum.TRANSFER_IN || tx.getTransactionType() == TransactionTypeEnum.RETURN) {
                    counts[0] += tx.getQuantity();
                } else if (tx.getTransactionType() == TransactionTypeEnum.STOCK_OUT || tx.getTransactionType() == TransactionTypeEnum.TRANSFER_OUT || tx.getTransactionType() == TransactionTypeEnum.DAMAGE) {
                    counts[1] += tx.getQuantity();
                }
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        List<DashboardSummaryResponse.StockMovementDto> result = new ArrayList<>();
        for (Map.Entry<LocalDate, long[]> entry : movementMap.entrySet()) {
            result.add(new DashboardSummaryResponse.StockMovementDto(
                    entry.getKey().format(formatter),
                    entry.getValue()[0],
                    entry.getValue()[1]
            ));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardSummaryResponse.CategoryValueDto> getInventoryByCategoryChart() {
        List<Category> categories = categoryRepository.findAll();
        List<Inventory> inventories = inventoryRepository.findAll();

        Map<Long, long[]> catUnitsMap = new HashMap<>(); // [units]
        Map<Long, BigDecimal> catValueMap = new HashMap<>();

        for (Category c : categories) {
            catUnitsMap.put(c.getId(), new long[]{0});
            catValueMap.put(c.getId(), BigDecimal.ZERO);
        }

        for (Inventory inv : inventories) {
            Long catId = inv.getProduct().getCategory().getId();
            long onHand = inv.getOnHandQuantity();
            BigDecimal cost = inv.getProduct().getCostPrice() != null ? inv.getProduct().getCostPrice() : BigDecimal.ZERO;
            BigDecimal value = cost.multiply(BigDecimal.valueOf(onHand));

            if (catUnitsMap.containsKey(catId)) {
                catUnitsMap.get(catId)[0] += onHand;
                catValueMap.put(catId, catValueMap.get(catId).add(value));
            }
        }

        List<DashboardSummaryResponse.CategoryValueDto> list = new ArrayList<>();
        for (Category c : categories) {
            list.add(new DashboardSummaryResponse.CategoryValueDto(
                    c.getName(),
                    catUnitsMap.get(c.getId())[0],
                    catValueMap.get(c.getId())
            ));
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardSummaryResponse.WarehouseStockDto> getInventoryByWarehouseChart() {
        List<Warehouse> warehouses = warehouseRepository.findAll();
        List<Inventory> inventories = inventoryRepository.findAll();

        Map<Long, long[]> whStock = new HashMap<>(); // [avail, res, dam, onhand]
        for (Warehouse w : warehouses) {
            whStock.put(w.getId(), new long[]{0, 0, 0, 0});
        }

        for (Inventory inv : inventories) {
            Long whId = inv.getWarehouse().getId();
            if (whStock.containsKey(whId)) {
                long[] stats = whStock.get(whId);
                stats[0] += (inv.getAvailableQuantity() != null ? inv.getAvailableQuantity() : 0);
                stats[1] += (inv.getReservedQuantity() != null ? inv.getReservedQuantity() : 0);
                stats[2] += (inv.getDamagedQuantity() != null ? inv.getDamagedQuantity() : 0);
                stats[3] += inv.getOnHandQuantity();
            }
        }

        List<DashboardSummaryResponse.WarehouseStockDto> result = new ArrayList<>();
        for (Warehouse w : warehouses) {
            long[] stats = whStock.get(w.getId());
            result.add(new DashboardSummaryResponse.WarehouseStockDto(
                    w.getName(),
                    w.getCode(),
                    stats[0],
                    stats[1],
                    stats[2],
                    stats[3]
            ));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardSummaryResponse.TopProductDto> getTopMovingProductsChart() {
        List<StockTransaction> transactions = stockTransactionRepository.findAll();

        Map<Long, long[]> statsMap = new HashMap<>(); // [txCount, totalQty]
        for (StockTransaction tx : transactions) {
            Long prodId = tx.getProduct().getId();
            statsMap.putIfAbsent(prodId, new long[]{0, 0});
            long[] cur = statsMap.get(prodId);
            cur[0] += 1;
            cur[1] += tx.getQuantity();
        }

        List<Product> products = productRepository.findAll();
        Map<Long, Product> prodMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));

        return statsMap.entrySet().stream()
                .map(e -> {
                    Product p = prodMap.get(e.getKey());
                    if (p == null) return null;
                    return new DashboardSummaryResponse.TopProductDto(
                            p.getId(),
                            p.getSku(),
                            p.getName(),
                            e.getValue()[0],
                            e.getValue()[1]
                    );
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> Long.compare(b.getTotalQuantityMoved(), a.getTotalQuantityMoved()))
                .limit(5)
                .collect(Collectors.toList());
    }
}
