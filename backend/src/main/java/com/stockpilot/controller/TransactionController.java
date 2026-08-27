package com.stockpilot.controller;

import com.stockpilot.dto.response.ApiResponse;
import com.stockpilot.dto.response.PagedResponse;
import com.stockpilot.dto.response.StockTransactionResponse;
import com.stockpilot.entity.TransactionTypeEnum;
import com.stockpilot.service.StockTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final StockTransactionService stockTransactionService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<StockTransactionResponse>>> getTransactions(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) TransactionTypeEnum type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PagedResponse<StockTransactionResponse> response = stockTransactionService.getTransactions(
                productId, warehouseId, type, startDate, endDate, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StockTransactionResponse>> getTransactionById(@PathVariable Long id) {
        StockTransactionResponse response = stockTransactionService.getTransactionById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping({"/product/{productId}", "/../products/{productId}/transactions"})
    public ResponseEntity<ApiResponse<List<StockTransactionResponse>>> getTransactionsByProduct(@PathVariable Long productId) {
        List<StockTransactionResponse> list = stockTransactionService.getTransactionsByProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping({"/warehouse/{warehouseId}", "/../warehouses/{warehouseId}/transactions"})
    public ResponseEntity<ApiResponse<List<StockTransactionResponse>>> getTransactionsByWarehouse(@PathVariable Long warehouseId) {
        List<StockTransactionResponse> list = stockTransactionService.getTransactionsByWarehouse(warehouseId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}
