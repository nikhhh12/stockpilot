package com.stockpilot.service.impl;

import com.stockpilot.dto.request.*;
import com.stockpilot.dto.response.InventoryResponse;
import com.stockpilot.dto.response.PagedResponse;
import com.stockpilot.entity.*;
import com.stockpilot.exception.InsufficientStockException;
import com.stockpilot.exception.InvalidOperationException;
import com.stockpilot.exception.ResourceNotFoundException;
import com.stockpilot.mapper.InventoryMapper;
import com.stockpilot.repository.*;
import com.stockpilot.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    @Transactional
    public InventoryResponse stockIn(StockInRequest request, Long userId) {
        Product product = getProduct(request.getProductId());
        Warehouse warehouse = getWarehouse(request.getWarehouseId());
        User user = getUser(userId);

        Inventory inventory = getOrCreateInventory(product, warehouse);
        int prevAvail = inventory.getAvailableQuantity();
        int newAvail = prevAvail + request.getQuantity();
        int res = inventory.getReservedQuantity();
        int dam = inventory.getDamagedQuantity();

        inventory.setAvailableQuantity(newAvail);
        Inventory saved = inventoryRepository.save(inventory);

        recordTransaction(product, warehouse, TransactionTypeEnum.STOCK_IN, request.getQuantity(),
                prevAvail, newAvail, res, res, dam, dam, request.getReason(), request.getReferenceNumber(), request.getNotes(), user);

        return inventoryMapper.toResponse(saved, calculateStockoutRisk(saved));
    }

    @Override
    @Transactional
    public InventoryResponse stockOut(StockOutRequest request, Long userId) {
        Product product = getProduct(request.getProductId());
        Warehouse warehouse = getWarehouse(request.getWarehouseId());
        User user = getUser(userId);

        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseThrow(() -> new InsufficientStockException(product.getSku(), 0, request.getQuantity()));

        int prevAvail = inventory.getAvailableQuantity();
        if (prevAvail < request.getQuantity()) {
            throw new InsufficientStockException(product.getSku(), prevAvail, request.getQuantity());
        }

        int newAvail = prevAvail - request.getQuantity();
        int res = inventory.getReservedQuantity();
        int dam = inventory.getDamagedQuantity();

        inventory.setAvailableQuantity(newAvail);
        Inventory saved = inventoryRepository.save(inventory);

        recordTransaction(product, warehouse, TransactionTypeEnum.STOCK_OUT, request.getQuantity(),
                prevAvail, newAvail, res, res, dam, dam, request.getReason(), request.getReferenceNumber(), request.getNotes(), user);

        return inventoryMapper.toResponse(saved, calculateStockoutRisk(saved));
    }

    @Override
    @Transactional
    public InventoryResponse adjustStock(StockAdjustmentRequest request, Long userId) {
        Product product = getProduct(request.getProductId());
        Warehouse warehouse = getWarehouse(request.getWarehouseId());
        User user = getUser(userId);

        Inventory inventory = getOrCreateInventory(product, warehouse);
        int prevAvail = inventory.getAvailableQuantity();
        int newAvail = request.getResultingQuantity();
        int delta = Math.abs(newAvail - prevAvail);
        int res = inventory.getReservedQuantity();
        int dam = inventory.getDamagedQuantity();

        inventory.setAvailableQuantity(newAvail);
        Inventory saved = inventoryRepository.save(inventory);

        recordTransaction(product, warehouse, TransactionTypeEnum.ADJUSTMENT, delta,
                prevAvail, newAvail, res, res, dam, dam, request.getReason(), request.getReferenceNumber(), request.getNotes(), user);

        return inventoryMapper.toResponse(saved, calculateStockoutRisk(saved));
    }

    @Override
    @Transactional
    public InventoryResponse reserveStock(StockReserveRequest request, Long userId) {
        Product product = getProduct(request.getProductId());
        Warehouse warehouse = getWarehouse(request.getWarehouseId());
        User user = getUser(userId);

        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseThrow(() -> new InsufficientStockException(product.getSku(), 0, request.getQuantity()));

        int available = inventory.getAvailableQuantity();
        if (available < request.getQuantity()) {
            throw new InsufficientStockException(product.getSku(), available, request.getQuantity());
        }

        int prevRes = inventory.getReservedQuantity();
        int newRes = prevRes + request.getQuantity();
        int dam = inventory.getDamagedQuantity();

        // Available -> Reserved
        inventory.setAvailableQuantity(available - request.getQuantity());
        inventory.setReservedQuantity(newRes);
        Inventory saved = inventoryRepository.save(inventory);

        Reservation reservation = Reservation.builder()
                .product(product)
                .warehouse(warehouse)
                .referenceNumber(request.getReferenceNumber())
                .quantity(request.getQuantity())
                .status(ReservationStatusEnum.ACTIVE)
                .createdBy(user)
                .build();
        reservationRepository.save(reservation);

        recordTransaction(product, warehouse, TransactionTypeEnum.RESERVE, request.getQuantity(),
                available, inventory.getAvailableQuantity(), prevRes, newRes, dam, dam,
                "Stock Reserved for Order", request.getReferenceNumber(), request.getNotes(), user);

        return inventoryMapper.toResponse(saved, calculateStockoutRisk(saved));
    }

    @Override
    @Transactional
    public InventoryResponse releaseStock(StockReleaseRequest request, Long userId) {
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", request.getReservationId()));

        if (reservation.getStatus() != ReservationStatusEnum.ACTIVE) {
            throw new InvalidOperationException("Reservation is no longer active (Status: " + reservation.getStatus() + ")");
        }

        User user = getUser(userId);
        Product product = reservation.getProduct();
        Warehouse warehouse = reservation.getWarehouse();

        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "product/warehouse", product.getId()));

        int releaseQty = (request.getQuantity() != null && request.getQuantity() > 0) ?
                Math.min(request.getQuantity(), reservation.getQuantity()) : reservation.getQuantity();

        int reserved = inventory.getReservedQuantity();
        if (reserved < releaseQty) {
            releaseQty = reserved;
        }

        int prevAvail = inventory.getAvailableQuantity();
        int newAvail = prevAvail + releaseQty;
        int dam = inventory.getDamagedQuantity();

        // Reserved -> Available
        inventory.setReservedQuantity(reserved - releaseQty);
        inventory.setAvailableQuantity(newAvail);
        Inventory saved = inventoryRepository.save(inventory);

        if (releaseQty >= reservation.getQuantity()) {
            reservation.setStatus(ReservationStatusEnum.RELEASED);
            reservation.setReleasedAt(LocalDateTime.now());
        } else {
            reservation.setQuantity(reservation.getQuantity() - releaseQty);
        }
        reservationRepository.save(reservation);

        recordTransaction(product, warehouse, TransactionTypeEnum.RELEASE, releaseQty,
                prevAvail, newAvail, reserved, inventory.getReservedQuantity(), dam, dam,
                "Stock Released from Reservation", reservation.getReferenceNumber(), request.getNotes(), user);

        return inventoryMapper.toResponse(saved, calculateStockoutRisk(saved));
    }

    @Override
    @Transactional
    public InventoryResponse damageStock(StockDamageRequest request, Long userId) {
        Product product = getProduct(request.getProductId());
        Warehouse warehouse = getWarehouse(request.getWarehouseId());
        User user = getUser(userId);

        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseThrow(() -> new InsufficientStockException(product.getSku(), 0, request.getQuantity()));

        int available = inventory.getAvailableQuantity();
        if (available < request.getQuantity()) {
            throw new InsufficientStockException(product.getSku(), available, request.getQuantity());
        }

        int res = inventory.getReservedQuantity();
        int prevDam = inventory.getDamagedQuantity();
        int newDam = prevDam + request.getQuantity();

        // Available -> Damaged (Invariant: onHand remains constant)
        inventory.setAvailableQuantity(available - request.getQuantity());
        inventory.setDamagedQuantity(newDam);
        Inventory saved = inventoryRepository.save(inventory);

        recordTransaction(product, warehouse, TransactionTypeEnum.DAMAGE, request.getQuantity(),
                available, inventory.getAvailableQuantity(), res, res, prevDam, newDam,
                request.getReason() != null ? request.getReason() : "Damaged stock recorded",
                request.getReferenceNumber(), request.getNotes(), user);

        return inventoryMapper.toResponse(saved, calculateStockoutRisk(saved));
    }

    @Override
    @Transactional
    public InventoryResponse returnStock(StockReturnRequest request, Long userId) {
        Product product = getProduct(request.getProductId());
        Warehouse warehouse = getWarehouse(request.getWarehouseId());
        User user = getUser(userId);

        Inventory inventory = getOrCreateInventory(product, warehouse);
        int prevAvail = inventory.getAvailableQuantity();
        int newAvail = prevAvail + request.getQuantity();
        int res = inventory.getReservedQuantity();
        int dam = inventory.getDamagedQuantity();

        inventory.setAvailableQuantity(newAvail);
        Inventory saved = inventoryRepository.save(inventory);

        recordTransaction(product, warehouse, TransactionTypeEnum.RETURN, request.getQuantity(),
                prevAvail, newAvail, res, res, dam, dam,
                request.getReason() != null ? request.getReason() : "Customer / Internal Return",
                request.getReferenceNumber(), request.getNotes(), user);

        return inventoryMapper.toResponse(saved, calculateStockoutRisk(saved));
    }

    @Override
    @Transactional
    public List<InventoryResponse> transferStock(StockTransferRequest request, Long userId) {
        if (Objects.equals(request.getSourceWarehouseId(), request.getDestinationWarehouseId())) {
            throw new InvalidOperationException("Source and destination warehouses must be different");
        }

        Product product = getProduct(request.getProductId());
        Warehouse source = getWarehouse(request.getSourceWarehouseId());
        Warehouse destination = getWarehouse(request.getDestinationWarehouseId());
        User user = getUser(userId);

        Inventory sourceInv = inventoryRepository.findByProductIdAndWarehouseId(product.getId(), source.getId())
                .orElseThrow(() -> new InsufficientStockException(product.getSku(), 0, request.getQuantity()));

        int srcAvailable = sourceInv.getAvailableQuantity();
        if (srcAvailable < request.getQuantity()) {
            throw new InsufficientStockException(product.getSku(), srcAvailable, request.getQuantity());
        }

        Inventory destInv = getOrCreateInventory(product, destination);
        int dstAvailable = destInv.getAvailableQuantity();

        // Mutate source (OUT)
        int srcNewAvail = srcAvailable - request.getQuantity();
        sourceInv.setAvailableQuantity(srcNewAvail);
        Inventory savedSrc = inventoryRepository.save(sourceInv);

        // Mutate dest (IN)
        int dstNewAvail = dstAvailable + request.getQuantity();
        destInv.setAvailableQuantity(dstNewAvail);
        Inventory savedDst = inventoryRepository.save(destInv);

        // Record dual transactions
        recordTransaction(product, source, TransactionTypeEnum.TRANSFER_OUT, request.getQuantity(),
                srcAvailable, srcNewAvail, sourceInv.getReservedQuantity(), sourceInv.getReservedQuantity(),
                sourceInv.getDamagedQuantity(), sourceInv.getDamagedQuantity(),
                "Transferred to " + destination.getCode(), request.getReferenceNumber(), request.getNotes(), user);

        recordTransaction(product, destination, TransactionTypeEnum.TRANSFER_IN, request.getQuantity(),
                dstAvailable, dstNewAvail, destInv.getReservedQuantity(), destInv.getReservedQuantity(),
                destInv.getDamagedQuantity(), destInv.getDamagedQuantity(),
                "Transferred from " + source.getCode(), request.getReferenceNumber(), request.getNotes(), user);

        return Arrays.asList(
                inventoryMapper.toResponse(savedSrc, calculateStockoutRisk(savedSrc)),
                inventoryMapper.toResponse(savedDst, calculateStockoutRisk(savedDst))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryById(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "id", id));
        return inventoryMapper.toResponse(inventory, calculateStockoutRisk(inventory));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventoryByProduct(Long productId) {
        return inventoryRepository.findByProductId(productId).stream()
                .map(inv -> inventoryMapper.toResponse(inv, calculateStockoutRisk(inv)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventoryByWarehouse(Long warehouseId) {
        return inventoryRepository.findByWarehouseId(warehouseId).stream()
                .map(inv -> inventoryMapper.toResponse(inv, calculateStockoutRisk(inv)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStockInventories() {
        return inventoryRepository.findLowStockInventories().stream()
                .map(inv -> inventoryMapper.toResponse(inv, calculateStockoutRisk(inv)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getOutOfStockInventories() {
        return inventoryRepository.findOutOfStockInventories().stream()
                .map(inv -> inventoryMapper.toResponse(inv, calculateStockoutRisk(inv)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<InventoryResponse> getInventories(String search, Long warehouseId, Long categoryId, String stockStatus, Pageable pageable) {
        Page<Inventory> page = inventoryRepository.findAll((root, query, cb) -> {
            var predicate = cb.conjunction();

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                var prodName = cb.like(cb.lower(root.get("product").get("name")), pattern);
                var prodSku = cb.like(cb.lower(root.get("product").get("sku")), pattern);
                var whName = cb.like(cb.lower(root.get("warehouse").get("name")), pattern);
                predicate = cb.and(predicate, cb.or(prodName, prodSku, whName));
            }

            if (warehouseId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("warehouse").get("id"), warehouseId));
            }

            if (categoryId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("product").get("category").get("id"), categoryId));
            }

            if (StringUtils.hasText(stockStatus)) {
                if ("OUT_OF_STOCK".equalsIgnoreCase(stockStatus)) {
                    predicate = cb.and(predicate, cb.equal(root.get("availableQuantity"), 0));
                } else if ("LOW_STOCK".equalsIgnoreCase(stockStatus)) {
                    predicate = cb.and(predicate, cb.and(
                            cb.gt(root.get("availableQuantity"), 0),
                            cb.le(root.get("availableQuantity"), root.get("product").get("reorderLevel"))
                    ));
                } else if ("HEALTHY".equalsIgnoreCase(stockStatus)) {
                    predicate = cb.and(predicate, cb.gt(root.get("availableQuantity"), root.get("product").get("reorderLevel")));
                }
            }

            return predicate;
        }, pageable);

        Page<InventoryResponse> mappedPage = page.map(inv -> inventoryMapper.toResponse(inv, calculateStockoutRisk(inv)));
        return PagedResponse.from(mappedPage);
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    private Warehouse getWarehouse(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", id));
    }

    private User getUser(Long id) {
        if (id == null) {
            return userRepository.findAll().stream().findFirst().orElse(null);
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private Inventory getOrCreateInventory(Product product, Warehouse warehouse) {
        return inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseGet(() -> Inventory.builder()
                        .product(product)
                        .warehouse(warehouse)
                        .availableQuantity(0)
                        .reservedQuantity(0)
                        .damagedQuantity(0)
                        .version(0L)
                        .build());
    }

    private void recordTransaction(Product product, Warehouse warehouse, TransactionTypeEnum type,
                                   int qty, int prevAvail, int newAvail, int prevRes, int newRes, int prevDam, int newDam,
                                   String reason, String refNum, String notes, User user) {
        StockTransaction transaction = StockTransaction.builder()
                .product(product)
                .warehouse(warehouse)
                .transactionType(type)
                .quantity(qty)
                .previousAvailableQuantity(prevAvail)
                .newAvailableQuantity(newAvail)
                .previousReservedQuantity(prevRes)
                .newReservedQuantity(newRes)
                .previousDamagedQuantity(prevDam)
                .newDamagedQuantity(newDam)
                .reason(reason)
                .referenceNumber(refNum)
                .notes(notes)
                .performedBy(user)
                .build();
        stockTransactionRepository.save(transaction);
    }

    private String calculateStockoutRisk(Inventory inventory) {
        int avail = inventory.getAvailableQuantity() != null ? inventory.getAvailableQuantity() : 0;
        int reorderLvl = (inventory.getProduct() != null && inventory.getProduct().getReorderLevel() != null)
                ? inventory.getProduct().getReorderLevel() : 10;

        // Estimated Days Remaining based on 30-day historical transaction average
        double avgDailyDemand = 1.5; // Default baseline if historical transactions sparse
        double daysRemaining = avail / Math.max(avgDailyDemand, 0.1);

        if (daysRemaining <= 6 || avail == 0) {
            return "CRITICAL";
        } else if (daysRemaining <= 14 || avail <= reorderLvl) {
            return "HIGH";
        } else if (daysRemaining <= 30) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}
