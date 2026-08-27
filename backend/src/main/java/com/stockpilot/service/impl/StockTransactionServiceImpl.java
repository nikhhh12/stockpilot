package com.stockpilot.service.impl;

import com.stockpilot.dto.response.PagedResponse;
import com.stockpilot.dto.response.StockTransactionResponse;
import com.stockpilot.entity.StockTransaction;
import com.stockpilot.entity.TransactionTypeEnum;
import com.stockpilot.exception.ResourceNotFoundException;
import com.stockpilot.mapper.StockTransactionMapper;
import com.stockpilot.repository.StockTransactionRepository;
import com.stockpilot.service.StockTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockTransactionServiceImpl implements StockTransactionService {

    private final StockTransactionRepository stockTransactionRepository;
    private final StockTransactionMapper stockTransactionMapper;

    @Override
    @Transactional(readOnly = true)
    public StockTransactionResponse getTransactionById(Long id) {
        StockTransaction st = stockTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockTransaction", "id", id));
        return stockTransactionMapper.toResponse(st);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockTransactionResponse> getTransactionsByProduct(Long productId) {
        return stockTransactionRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(stockTransactionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockTransactionResponse> getTransactionsByWarehouse(Long warehouseId) {
        return stockTransactionRepository.findByWarehouseIdOrderByCreatedAtDesc(warehouseId).stream()
                .map(stockTransactionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<StockTransactionResponse> getTransactions(
            Long productId, Long warehouseId, TransactionTypeEnum type,
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable
    ) {
        Page<StockTransaction> page = stockTransactionRepository.findAll((root, query, cb) -> {
            var predicate = cb.conjunction();

            if (productId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("product").get("id"), productId));
            }
            if (warehouseId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("warehouse").get("id"), warehouseId));
            }
            if (type != null) {
                predicate = cb.and(predicate, cb.equal(root.get("transactionType"), type));
            }
            if (startDate != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return predicate;
        }, pageable);

        Page<StockTransactionResponse> mappedPage = page.map(stockTransactionMapper::toResponse);
        return PagedResponse.from(mappedPage);
    }
}
