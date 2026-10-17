package com.stockpilot.service;

import com.stockpilot.dto.response.AnomalyDetectionResponse;
import com.stockpilot.dto.response.DemandForecastResponse;
import com.stockpilot.dto.response.ReorderRecommendationResponse;
import com.stockpilot.dto.response.StockoutRiskResponse;
import com.stockpilot.entity.*;
import com.stockpilot.repository.*;
import com.stockpilot.service.forecast.ExponentialSmoothingForecastAlgorithm;
import com.stockpilot.service.forecast.MovingAverageForecastAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
public class StatisticalIntelligenceTest {

    @Autowired
    private DemandForecastService demandForecastService;

    @Autowired
    private StockoutRiskService stockoutRiskService;

    @Autowired
    private ReorderRecommendationService reorderRecommendationService;

    @Autowired
    private AnomalyDetectionService anomalyDetectionService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovingAverageForecastAlgorithm movingAverageAlgorithm;

    @Autowired
    private ExponentialSmoothingForecastAlgorithm exponentialSmoothingAlgorithm;

    private Product product;
    private Warehouse warehouse;
    private User user;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> categoryRepository.save(Category.builder().name("Analytics Cat").build()));

        Supplier supplier = supplierRepository.findAll().stream().findFirst()
                .orElseGet(() -> supplierRepository.save(Supplier.builder().companyName("Analytics Supplier").build()));

        product = productRepository.save(Product.builder()
                .sku("ANALYTICS-SKU-" + System.currentTimeMillis())
                .name("Analytics Test SKU")
                .category(category)
                .supplier(supplier)
                .costPrice(BigDecimal.valueOf(50))
                .sellingPrice(BigDecimal.valueOf(90))
                .unit("unit")
                .reorderLevel(10)
                .reorderQuantity(40)
                .active(true)
                .build());

        warehouse = warehouseRepository.findAll().stream().findFirst()
                .orElseGet(() -> warehouseRepository.save(Warehouse.builder().name("Analytics Hub").code("ANL-WH").build()));

        user = userRepository.findAll().stream().findFirst()
                .orElseGet(() -> userRepository.save(User.builder().name("Tester").email("tester@test.com").passwordHash("pwd").role(RoleEnum.ADMIN).build()));
    }

    @Test
    @DisplayName("Demand Forecasting returns INSUFFICIENT_DATA when observations < 3")
    void testDemandForecastInsufficientData() {
        DemandForecastResponse response = demandForecastService.getProductDemandForecast(product.getId(), "MOVING_AVERAGE", "14_DAYS");
        assertEquals("INSUFFICIENT_DATA", response.getStatus());
        assertEquals(0.0, response.getPredictedDemand());
    }

    @Test
    @DisplayName("Demand Forecasting computes moving average and exponential smoothing with adequate data")
    void testDemandForecastWithData() {
        // Record 5 distinct consumption transactions
        for (int i = 1; i <= 5; i++) {
            StockTransaction tx = StockTransaction.builder()
                    .product(product)
                    .warehouse(warehouse)
                    .transactionType(TransactionTypeEnum.STOCK_OUT)
                    .quantity(10)
                    .previousAvailableQuantity(100 - (i * 10))
                    .newAvailableQuantity(90 - (i * 10))
                    .previousReservedQuantity(0)
                    .newReservedQuantity(0)
                    .previousDamagedQuantity(0)
                    .newDamagedQuantity(0)
                    .performedBy(user)
                    .createdAt(LocalDateTime.now().minusDays(i * 3))
                    .build();
            stockTransactionRepository.save(tx);
        }

        DemandForecastResponse maResponse = demandForecastService.getProductDemandForecast(product.getId(), "MOVING_AVERAGE", "14_DAYS");
        assertEquals("OK", maResponse.getStatus());
        assertTrue(maResponse.getPredictedDemand() > 0);
        assertEquals("MOVING_AVERAGE", maResponse.getAlgorithm());

        DemandForecastResponse esResponse = demandForecastService.getProductDemandForecast(product.getId(), "EXPONENTIAL_SMOOTHING", "14_DAYS");
        assertEquals("OK", esResponse.getStatus());
        assertTrue(esResponse.getPredictedDemand() > 0);
        assertEquals("EXPONENTIAL_SMOOTHING", esResponse.getAlgorithm());
    }

    @Test
    @DisplayName("Stockout Risk classifies CRITICAL, HIGH, MEDIUM, and LOW boundaries precisely")
    void testStockoutRiskBoundaries() {
        Inventory inv = inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseGet(() -> inventoryRepository.save(Inventory.builder().product(product).warehouse(warehouse).availableQuantity(0).reservedQuantity(0).damagedQuantity(0).build()));

        // 1. Available = 0 -> CRITICAL
        inv.setAvailableQuantity(0);
        inventoryRepository.save(inv);
        StockoutRiskResponse r1 = stockoutRiskService.calculateProductStockoutRisk(product.getId());
        assertEquals("CRITICAL", r1.getRiskLevel());

        // 2. Available = 5 (<= 6 days buffer @ 1.0 demand) -> CRITICAL
        inv.setAvailableQuantity(5);
        inventoryRepository.save(inv);
        StockoutRiskResponse r2 = stockoutRiskService.calculateProductStockoutRisk(product.getId());
        assertEquals("CRITICAL", r2.getRiskLevel());

        // 3. Available = 12 (<= 14 days buffer) -> HIGH
        inv.setAvailableQuantity(12);
        inventoryRepository.save(inv);
        StockoutRiskResponse r3 = stockoutRiskService.calculateProductStockoutRisk(product.getId());
        assertEquals("HIGH", r3.getRiskLevel());

        // 4. Available = 25 (<= 30 days buffer) -> MEDIUM
        inv.setAvailableQuantity(25);
        inventoryRepository.save(inv);
        StockoutRiskResponse r4 = stockoutRiskService.calculateProductStockoutRisk(product.getId());
        assertEquals("MEDIUM", r4.getRiskLevel());

        // 5. Available = 50 (> 30 days buffer) -> LOW
        inv.setAvailableQuantity(50);
        inventoryRepository.save(inv);
        StockoutRiskResponse r5 = stockoutRiskService.calculateProductStockoutRisk(product.getId());
        assertEquals("LOW", r5.getRiskLevel());
    }

    @Test
    @DisplayName("Reorder Recommendation evaluates safety stock, lead time demand, and inventory position")
    void testReorderRecommendation() {
        Inventory inv = inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseGet(() -> inventoryRepository.save(Inventory.builder().product(product).warehouse(warehouse).availableQuantity(0).reservedQuantity(0).damagedQuantity(0).build()));
        inv.setAvailableQuantity(3);
        inventoryRepository.save(inv);

        ReorderRecommendationResponse rec = reorderRecommendationService.getProductReorderRecommendation(product.getId());
        assertNotNull(rec);
        assertTrue(rec.getRecommendedReorderQuantity() > 0, "Reorder quantity should be positive");
        assertTrue("CRITICAL".equals(rec.getUrgency()) || "HIGH".equals(rec.getUrgency()));
    }

    @Test
    @DisplayName("Anomaly Detection returns NO_VARIANCE when stdDev == 0 and detects anomalies when z >= 2.0")
    void testAnomalyDetection() {
        // Create baseline of identical transactions (zero variance)
        for (int i = 0; i < 4; i++) {
            stockTransactionRepository.save(StockTransaction.builder()
                    .product(product)
                    .warehouse(warehouse)
                    .transactionType(TransactionTypeEnum.STOCK_OUT)
                    .quantity(10)
                    .previousAvailableQuantity(100)
                    .newAvailableQuantity(90)
                    .previousReservedQuantity(0)
                    .newReservedQuantity(0)
                    .previousDamagedQuantity(0)
                    .newDamagedQuantity(0)
                    .performedBy(user)
                    .createdAt(LocalDateTime.now().minusDays(i + 1))
                    .build());
        }

        List<StockTransaction> allTx = stockTransactionRepository.findAll().stream()
                .filter(t -> t.getProduct().getId().equals(product.getId()))
                .toList();

        AnomalyDetectionResponse zeroVarReport = anomalyDetectionService.analyzeTransaction(allTx.get(0).getId());
        assertEquals("NO_VARIANCE", zeroVarReport.getStatus(), "Must return NO_VARIANCE when stdDev is 0");
        assertNull(zeroVarReport.getAbsoluteZScore());

        // Now add a massive outlier spike transaction (quantity = 150 vs mean = 10)
        StockTransaction spikeTx = stockTransactionRepository.save(StockTransaction.builder()
                .product(product)
                .warehouse(warehouse)
                .transactionType(TransactionTypeEnum.STOCK_OUT)
                .quantity(150)
                .previousAvailableQuantity(200)
                .newAvailableQuantity(50)
                .previousReservedQuantity(0)
                .newReservedQuantity(0)
                .previousDamagedQuantity(0)
                .newDamagedQuantity(0)
                .performedBy(user)
                .createdAt(LocalDateTime.now())
                .build());

        AnomalyDetectionResponse anomalyReport = anomalyDetectionService.analyzeTransaction(spikeTx.getId());
        assertEquals("STATISTICAL_ANOMALY", anomalyReport.getStatus());
        assertTrue(anomalyReport.getAbsoluteZScore() >= 2.0);
    }

    @Test
    @DisplayName("Mathematical algorithms handle null items, zero horizon, and exact hand-calculated figures")
    void testForecastingAlgorithmEdgeCasesWithNullsAndZeroDays() {
        MovingAverageForecastAlgorithm ma = new MovingAverageForecastAlgorithm();
        ExponentialSmoothingForecastAlgorithm es = new ExponentialSmoothingForecastAlgorithm();

        // Empty / Zero Horizon guards
        assertEquals(0.0, ma.calculateForecast(null, 14));
        assertEquals(0.0, ma.calculateForecast(java.util.Collections.emptyList(), 14));
        assertEquals(0.0, ma.calculateForecast(java.util.List.of(10.0, 20.0), 0));
        assertEquals(0.0, ma.calculateForecast(java.util.List.of(10.0, 20.0), -5));

        assertEquals(0.0, es.calculateForecast(null, 14));
        assertEquals(0.0, es.calculateForecast(java.util.Collections.emptyList(), 14));
        assertEquals(0.0, es.calculateForecast(java.util.List.of(10.0, 20.0), 0));

        // Null elements in demand list
        java.util.List<Double> withNulls = java.util.Arrays.asList(10.0, null, 20.0);
        Double maNullResult = ma.calculateForecast(withNulls, 3);
        assertNotNull(maNullResult);
        assertEquals(30.0, maNullResult); // sum=30, size=3, avg=10, 3 days = 30.0

        Double esNullResult = es.calculateForecast(withNulls, 1);
        assertNotNull(esNullResult);
        assertTrue(esNullResult > 0);

        // Hand calculation verification for Moving Average: [10.0, 20.0, 30.0], 7 days -> mean = 20, 20*7 = 140.0
        java.util.List<Double> handValues = java.util.List.of(10.0, 20.0, 30.0);
        assertEquals(140.0, ma.calculateForecast(handValues, 7));
    }
}
