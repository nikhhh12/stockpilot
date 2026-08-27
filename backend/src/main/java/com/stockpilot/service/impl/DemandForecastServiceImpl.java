package com.stockpilot.service.impl;

import com.stockpilot.dto.response.DemandForecastResponse;
import com.stockpilot.entity.Product;
import com.stockpilot.entity.StockTransaction;
import com.stockpilot.entity.TransactionTypeEnum;
import com.stockpilot.exception.ResourceNotFoundException;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.StockTransactionRepository;
import com.stockpilot.service.DemandForecastService;
import com.stockpilot.service.forecast.ExponentialSmoothingForecastAlgorithm;
import com.stockpilot.service.forecast.ForecastAlgorithm;
import com.stockpilot.service.forecast.MovingAverageForecastAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemandForecastServiceImpl implements DemandForecastService {

    private final ProductRepository productRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final MovingAverageForecastAlgorithm movingAverageAlgorithm;
    private final ExponentialSmoothingForecastAlgorithm exponentialSmoothingAlgorithm;

    @Override
    @Transactional(readOnly = true)
    public DemandForecastResponse getProductDemandForecast(Long productId, String algorithmStr, String horizonStr) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        int horizonDays = parseHorizonDays(horizonStr);
        String horizon = horizonDays + "_DAYS";

        ForecastAlgorithm algorithm = "EXPONENTIAL_SMOOTHING".equalsIgnoreCase(algorithmStr)
                ? exponentialSmoothingAlgorithm
                : movingAverageAlgorithm;

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<StockTransaction> transactions = stockTransactionRepository.findAll().stream()
                .filter(t -> t.getProduct().getId().equals(productId) &&
                        (t.getTransactionType() == TransactionTypeEnum.STOCK_OUT || t.getTransactionType() == TransactionTypeEnum.RESERVE) &&
                        t.getCreatedAt().isAfter(thirtyDaysAgo))
                .sorted(Comparator.comparing(StockTransaction::getCreatedAt))
                .collect(Collectors.toList());

        // Group demand by date over the past 30 days
        Map<LocalDate, Integer> dailyDemandMap = new LinkedHashMap<>();
        LocalDate startDate = LocalDate.now().minusDays(29);
        for (int i = 0; i < 30; i++) {
            dailyDemandMap.put(startDate.plusDays(i), 0);
        }

        for (StockTransaction tx : transactions) {
            LocalDate txDate = tx.getCreatedAt().toLocalDate();
            if (dailyDemandMap.containsKey(txDate)) {
                dailyDemandMap.put(txDate, dailyDemandMap.get(txDate) + tx.getQuantity());
            }
        }

        List<DemandForecastResponse.DailyDemandPoint> historicalData = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        List<Double> demandValues = new ArrayList<>();

        for (Map.Entry<LocalDate, Integer> entry : dailyDemandMap.entrySet()) {
            historicalData.add(new DemandForecastResponse.DailyDemandPoint(
                    entry.getKey().format(formatter),
                    entry.getValue()
            ));
            demandValues.add(entry.getValue().doubleValue());
        }

        if (transactions.size() < 3) {
            return DemandForecastResponse.builder()
                    .productId(product.getId())
                    .productSku(product.getSku())
                    .productName(product.getName())
                    .forecastHorizon(horizon)
                    .averageDailyDemand(0.0)
                    .predictedDemand(0.0)
                    .algorithm(algorithm.getAlgorithmName())
                    .dataPointsUsed(transactions.size())
                    .status("INSUFFICIENT_DATA")
                    .message("Insufficient historical demand observations to calculate statistical forecast (minimum 3 distinct consumption periods required).")
                    .historicalData(historicalData)
                    .build();
        }

        double totalHistoricalDemand = transactions.stream().mapToInt(StockTransaction::getQuantity).sum();
        double avgDailyDemand = Math.round((totalHistoricalDemand / 30.0) * 100.0) / 100.0;
        double predicted = algorithm.calculateForecast(demandValues, horizonDays);

        return DemandForecastResponse.builder()
                .productId(product.getId())
                .productSku(product.getSku())
                .productName(product.getName())
                .forecastHorizon(horizon)
                .averageDailyDemand(avgDailyDemand)
                .predictedDemand(predicted)
                .algorithm(algorithm.getAlgorithmName())
                .dataPointsUsed(transactions.size())
                .status("OK")
                .message("Forecast computed successfully using " + algorithm.getAlgorithmName() + " over " + transactions.size() + " consumption events.")
                .historicalData(historicalData)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandForecastResponse> getAllDemandForecasts(String horizon) {
        return productRepository.findAll().stream()
                .filter(Product::isActive)
                .map(product -> getProductDemandForecast(product.getId(), "MOVING_AVERAGE", horizon))
                .collect(Collectors.toList());
    }

    private int parseHorizonDays(String horizonStr) {
        if (horizonStr == null) return 14;
        if (horizonStr.contains("7")) return 7;
        if (horizonStr.contains("30")) return 30;
        return 14;
    }
}
