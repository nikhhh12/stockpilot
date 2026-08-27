package com.stockpilot.service.impl;

import com.stockpilot.dto.response.StockoutRiskResponse;
import com.stockpilot.entity.Inventory;
import com.stockpilot.entity.Product;
import com.stockpilot.entity.StockTransaction;
import com.stockpilot.entity.TransactionTypeEnum;
import com.stockpilot.exception.ResourceNotFoundException;
import com.stockpilot.repository.InventoryRepository;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.StockTransactionRepository;
import com.stockpilot.service.StockoutRiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockoutRiskServiceImpl implements StockoutRiskService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StockTransactionRepository stockTransactionRepository;

    @Value("${stockpilot.risk.critical-threshold-days:6}")
    private double criticalThresholdDays;

    @Value("${stockpilot.risk.high-threshold-days:14}")
    private double highThresholdDays;

    @Value("${stockpilot.risk.medium-threshold-days:30}")
    private double mediumThresholdDays;

    @Override
    @Transactional(readOnly = true)
    public StockoutRiskResponse calculateProductStockoutRisk(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        List<Inventory> inventories = inventoryRepository.findByProductId(productId);
        int totalAvailable = inventories.stream()
                .mapToInt(inv -> inv.getAvailableQuantity() != null ? inv.getAvailableQuantity() : 0)
                .sum();

        // Calculate average daily demand from past 30 days of consumption
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<StockTransaction> consumptionTx = stockTransactionRepository.findAll().stream()
                .filter(t -> t.getProduct().getId().equals(productId) &&
                        (t.getTransactionType() == TransactionTypeEnum.STOCK_OUT || t.getTransactionType() == TransactionTypeEnum.RESERVE) &&
                        t.getCreatedAt().isAfter(thirtyDaysAgo))
                .collect(Collectors.toList());

        int totalConsumed = consumptionTx.stream().mapToInt(StockTransaction::getQuantity).sum();
        double avgDailyDemand = totalConsumed > 0 ? (double) totalConsumed / 30.0 : 1.0;
        avgDailyDemand = Math.round(avgDailyDemand * 100.0) / 100.0;

        double daysRemaining;
        if (totalAvailable <= 0) {
            daysRemaining = 0.0;
        } else {
            daysRemaining = Math.round((totalAvailable / avgDailyDemand) * 10.0) / 10.0;
        }

        String riskLevel = classifyRiskLevel(daysRemaining, totalAvailable, product.getReorderLevel());
        String explanation = generateRiskExplanation(product, totalAvailable, daysRemaining, riskLevel);

        return StockoutRiskResponse.builder()
                .productId(product.getId())
                .productSku(product.getSku())
                .productName(product.getName())
                .availableQuantity(totalAvailable)
                .averageDailyDemand(avgDailyDemand)
                .estimatedDaysRemaining(daysRemaining)
                .reorderLevel(product.getReorderLevel())
                .riskLevel(riskLevel)
                .explanation(explanation)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockoutRiskResponse> getAllStockoutRisks() {
        return productRepository.findAll().stream()
                .filter(Product::isActive)
                .map(p -> calculateProductStockoutRisk(p.getId()))
                .sorted(Comparator.comparing(StockoutRiskResponse::getEstimatedDaysRemaining))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockoutRiskResponse> getCriticalStockoutRisks() {
        return getAllStockoutRisks().stream()
                .filter(r -> "CRITICAL".equals(r.getRiskLevel()) || "HIGH".equals(r.getRiskLevel()))
                .collect(Collectors.toList());
    }

    private String classifyRiskLevel(double daysRemaining, int available, int reorderLevel) {
        if (available == 0 || daysRemaining <= criticalThresholdDays) {
            return "CRITICAL";
        } else if (daysRemaining <= highThresholdDays || available <= reorderLevel) {
            return "HIGH";
        } else if (daysRemaining <= mediumThresholdDays) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private String generateRiskExplanation(Product product, int available, double daysRemaining, String riskLevel) {
        if (available == 0) {
            return String.format("Stockout alert! '%s' has 0 available units across all fulfillment hubs.", product.getName());
        }
        switch (riskLevel) {
            case "CRITICAL":
                return String.format("Critical stockout risk! Estimated %s days of supply remaining at current velocity. Immediate purchase order recommended.", daysRemaining);
            case "HIGH":
                return String.format("High stockout risk. Available inventory (%d units) is approaching safety reorder threshold (%d units).", available, product.getReorderLevel());
            case "MEDIUM":
                return String.format("Moderate inventory buffer. %s days of stock remaining. Monitor supplier lead times.", daysRemaining);
            case "LOW":
            default:
                return String.format("Healthy stock level. %s days of coverage remaining.", daysRemaining);
        }
    }
}
