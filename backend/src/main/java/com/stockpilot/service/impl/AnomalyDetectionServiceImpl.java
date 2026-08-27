package com.stockpilot.service.impl;

import com.stockpilot.dto.response.AnomalyDetectionResponse;
import com.stockpilot.entity.StockTransaction;
import com.stockpilot.exception.ResourceNotFoundException;
import com.stockpilot.repository.StockTransactionRepository;
import com.stockpilot.service.AnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionServiceImpl implements AnomalyDetectionService {

    private final StockTransactionRepository stockTransactionRepository;

    private static final double Z_SCORE_ANOMALY_THRESHOLD = 2.0;
    private static final double Z_SCORE_CRITICAL_THRESHOLD = 3.0;

    @Override
    @Transactional(readOnly = true)
    public List<AnomalyDetectionResponse> detectTransactionAnomalies() {
        List<StockTransaction> allTransactions = stockTransactionRepository.findAll();
        if (allTransactions.isEmpty()) {
            return new ArrayList<>();
        }

        // Group transactions by product ID to evaluate product-specific statistical distributions
        Map<Long, List<StockTransaction>> txByProduct = allTransactions.stream()
                .collect(Collectors.groupingBy(t -> t.getProduct().getId()));

        List<AnomalyDetectionResponse> anomalies = new ArrayList<>();

        for (Map.Entry<Long, List<StockTransaction>> entry : txByProduct.entrySet()) {
            List<StockTransaction> productTx = entry.getValue();
            for (StockTransaction tx : productTx) {
                AnomalyDetectionResponse report = evaluateTransaction(tx, productTx);
                if ("STATISTICAL_ANOMALY".equals(report.getStatus())) {
                    anomalies.add(report);
                }
            }
        }

        return anomalies.stream()
                .sorted(Comparator.comparing(AnomalyDetectionResponse::getAbsoluteZScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AnomalyDetectionResponse analyzeTransaction(Long transactionId) {
        StockTransaction tx = stockTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("StockTransaction", "id", transactionId));

        List<StockTransaction> productTx = stockTransactionRepository.findAll().stream()
                .filter(t -> t.getProduct().getId().equals(tx.getProduct().getId()))
                .collect(Collectors.toList());

        return evaluateTransaction(tx, productTx);
    }

    private AnomalyDetectionResponse evaluateTransaction(StockTransaction tx, List<StockTransaction> baseline) {
        if (baseline == null || baseline.size() < 3) {
            return AnomalyDetectionResponse.builder()
                    .transactionId(tx.getId())
                    .productId(tx.getProduct().getId())
                    .productSku(tx.getProduct().getSku())
                    .productName(tx.getProduct().getName())
                    .warehouseId(tx.getWarehouse().getId())
                    .warehouseName(tx.getWarehouse().getName())
                    .transactionType(tx.getTransactionType().name())
                    .observedQuantity(tx.getQuantity())
                    .expectedMeanQuantity((double) tx.getQuantity())
                    .standardDeviation(0.0)
                    .absoluteZScore(0.0)
                    .status("INSUFFICIENT_DATA")
                    .severity("LOW")
                    .reason("Insufficient sample size to compute reliable baseline statistics (minimum 3 records required).")
                    .transactionDate(tx.getCreatedAt())
                    .build();
        }

        // Calculate Mean
        double sum = 0.0;
        for (StockTransaction t : baseline) {
            sum += t.getQuantity();
        }
        double mean = sum / baseline.size();

        // Calculate Variance and Standard Deviation
        double sumSqDiff = 0.0;
        for (StockTransaction t : baseline) {
            sumSqDiff += Math.pow(t.getQuantity() - mean, 2);
        }
        double variance = sumSqDiff / baseline.size();
        double standardDeviation = Math.sqrt(variance);

        // Explicit Section 1.1 Invariant: If standardDeviation == 0, return NO_VARIANCE
        if (standardDeviation == 0.0) {
            return AnomalyDetectionResponse.builder()
                    .transactionId(tx.getId())
                    .productId(tx.getProduct().getId())
                    .productSku(tx.getProduct().getSku())
                    .productName(tx.getProduct().getName())
                    .warehouseId(tx.getWarehouse().getId())
                    .warehouseName(tx.getWarehouse().getName())
                    .transactionType(tx.getTransactionType().name())
                    .observedQuantity(tx.getQuantity())
                    .expectedMeanQuantity(Math.round(mean * 100.0) / 100.0)
                    .standardDeviation(0.0)
                    .absoluteZScore(null)
                    .status("NO_VARIANCE")
                    .severity("LOW")
                    .reason("Zero standard deviation across baseline sample. All transaction quantities are identical; anomaly classification bypassed.")
                    .transactionDate(tx.getCreatedAt())
                    .build();
        }

        // Exact Formula: absoluteZScore = |X - mean| / standardDeviation
        double absoluteZScore = Math.abs(tx.getQuantity() - mean) / standardDeviation;
        absoluteZScore = Math.round(absoluteZScore * 100.0) / 100.0;

        String status = (absoluteZScore >= Z_SCORE_ANOMALY_THRESHOLD) ? "STATISTICAL_ANOMALY" : "NORMAL";
        String severity;
        if (absoluteZScore >= Z_SCORE_CRITICAL_THRESHOLD) {
            severity = "HIGH";
        } else if (absoluteZScore >= Z_SCORE_ANOMALY_THRESHOLD) {
            severity = "MEDIUM";
        } else {
            severity = "LOW";
        }

        String reason = status.equals("STATISTICAL_ANOMALY")
                ? String.format("Transaction quantity of %d deviates significantly from historical mean of %.1f units (Z-Score: %.2f, σ: %.2f).",
                tx.getQuantity(), mean, absoluteZScore, standardDeviation)
                : "Transaction volume aligns within normal statistical tolerance boundaries.";

        return AnomalyDetectionResponse.builder()
                .transactionId(tx.getId())
                .productId(tx.getProduct().getId())
                .productSku(tx.getProduct().getSku())
                .productName(tx.getProduct().getName())
                .warehouseId(tx.getWarehouse().getId())
                .warehouseName(tx.getWarehouse().getName())
                .transactionType(tx.getTransactionType().name())
                .observedQuantity(tx.getQuantity())
                .expectedMeanQuantity(Math.round(mean * 100.0) / 100.0)
                .standardDeviation(Math.round(standardDeviation * 100.0) / 100.0)
                .absoluteZScore(absoluteZScore)
                .status(status)
                .severity(severity)
                .reason(reason)
                .transactionDate(tx.getCreatedAt())
                .build();
    }
}
