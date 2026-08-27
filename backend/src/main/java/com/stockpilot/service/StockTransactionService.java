package com.stockpilot.service;

import com.stockpilot.dto.response.PagedResponse;
import com.stockpilot.dto.response.StockTransactionResponse;
import com.stockpilot.entity.TransactionTypeEnum;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface StockTransactionService {
    StockTransactionResponse getTransactionById(Long id);
    List<StockTransactionResponse> getTransactionsByProduct(Long productId);
    List<StockTransactionResponse> getTransactionsByWarehouse(Long warehouseId);
    PagedResponse<StockTransactionResponse> getTransactions(
            Long productId, Long warehouseId, TransactionTypeEnum type,
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable
    );
}
