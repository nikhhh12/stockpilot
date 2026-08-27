package com.stockpilot.service;

import com.stockpilot.dto.response.AnomalyDetectionResponse;

import java.util.List;

public interface AnomalyDetectionService {
    List<AnomalyDetectionResponse> detectTransactionAnomalies();
    AnomalyDetectionResponse analyzeTransaction(Long transactionId);
}
