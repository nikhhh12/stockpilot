package com.stockpilot.service.impl;

import com.stockpilot.dto.request.PurchaseOrderCreateRequest;
import com.stockpilot.dto.request.PurchaseOrderItemReceiveDto;
import com.stockpilot.dto.request.PurchaseOrderItemRequest;
import com.stockpilot.dto.request.PurchaseOrderReceiveRequest;
import com.stockpilot.dto.response.PagedResponse;
import com.stockpilot.dto.response.PurchaseOrderResponse;
import com.stockpilot.entity.*;
import com.stockpilot.exception.InvalidOperationException;
import com.stockpilot.exception.ResourceNotFoundException;
import com.stockpilot.mapper.PurchaseOrderMapper;
import com.stockpilot.repository.*;
import com.stockpilot.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final com.stockpilot.service.AuditLogService auditLogService;

    @Override
    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderCreateRequest request, Long userId) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", request.getSupplierId()));

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", request.getWarehouseId()));

        User user = getUser(userId);
        String poNumber = "PO-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PurchaseOrderItem> items = new ArrayList<>();

        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .poNumber(poNumber)
                .supplier(supplier)
                .warehouse(warehouse)
                .status(PurchaseOrderStatusEnum.PENDING)
                .orderDate(LocalDate.now())
                .expectedDeliveryDate(request.getExpectedDeliveryDate() != null ? request.getExpectedDeliveryDate() : LocalDate.now().plusDays(7))
                .notes(request.getNotes())
                .totalAmount(BigDecimal.ZERO)
                .createdBy(user)
                .items(items)
                .build();

        for (PurchaseOrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemReq.getProductId()));

            BigDecimal totalCost = itemReq.getUnitCost().multiply(BigDecimal.valueOf(itemReq.getOrderedQuantity()));
            totalAmount = totalAmount.add(totalCost);

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .purchaseOrder(purchaseOrder)
                    .product(product)
                    .orderedQuantity(itemReq.getOrderedQuantity())
                    .receivedQuantity(0)
                    .unitCost(itemReq.getUnitCost())
                    .totalCost(totalCost)
                    .build();

            items.add(item);
        }

        purchaseOrder.setTotalAmount(totalAmount);
        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        return purchaseOrderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponse getPurchaseOrderById(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));
        return purchaseOrderMapper.toResponse(po);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll().stream()
                .map(purchaseOrderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PurchaseOrderResponse> getPurchaseOrders(
            String search, Long supplierId, Long warehouseId, PurchaseOrderStatusEnum status, Pageable pageable
    ) {
        Page<PurchaseOrder> page = purchaseOrderRepository.findAll((root, query, cb) -> {
            var predicate = cb.conjunction();

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                var poNum = cb.like(cb.lower(root.get("poNumber")), pattern);
                var supp = cb.like(cb.lower(root.get("supplier").get("companyName")), pattern);
                predicate = cb.and(predicate, cb.or(poNum, supp));
            }
            if (supplierId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("supplier").get("id"), supplierId));
            }
            if (warehouseId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("warehouse").get("id"), warehouseId));
            }
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }

            return predicate;
        }, pageable);

        Page<PurchaseOrderResponse> mappedPage = page.map(purchaseOrderMapper::toResponse);
        return PagedResponse.from(mappedPage);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse approvePurchaseOrder(Long id, Long userId) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        if (po.getStatus() != PurchaseOrderStatusEnum.PENDING && po.getStatus() != PurchaseOrderStatusEnum.DRAFT) {
            throw new InvalidOperationException("Cannot approve PO in status: " + po.getStatus());
        }

        po.setStatus(PurchaseOrderStatusEnum.APPROVED);
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        User actor = getUser(userId);
        auditLogService.logAction(actor, "APPROVE_PO", "PurchaseOrder", saved.getId(), "Approved purchase order " + saved.getPoNumber());
        return purchaseOrderMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse orderPurchaseOrder(Long id, Long userId) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        if (po.getStatus() != PurchaseOrderStatusEnum.APPROVED) {
            throw new InvalidOperationException("Cannot place order for PO in status: " + po.getStatus() + ". PO must be APPROVED first.");
        }

        po.setStatus(PurchaseOrderStatusEnum.ORDERED);
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        User actor = getUser(userId);
        auditLogService.logAction(actor, "ORDER_PO", "PurchaseOrder", saved.getId(), "Placed purchase order " + saved.getPoNumber());
        return purchaseOrderMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse receivePurchaseOrder(Long id, PurchaseOrderReceiveRequest request, Long userId) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        if (po.getStatus() != PurchaseOrderStatusEnum.APPROVED &&
                po.getStatus() != PurchaseOrderStatusEnum.ORDERED &&
                po.getStatus() != PurchaseOrderStatusEnum.PARTIALLY_RECEIVED) {
            throw new InvalidOperationException("Cannot receive items for PO in status: " + po.getStatus());
        }

        // Idempotency check: if client provided a receipt reference that was already processed
        if (StringUtils.hasText(request.getReceiptReference())) {
            var existingTxOpt = stockTransactionRepository.findFirstByReferenceNumberAndTransactionType(
                    request.getReceiptReference(), TransactionTypeEnum.STOCK_IN);
            if (existingTxOpt.isPresent()) {
                StockTransaction prevTx = existingTxOpt.get();
                if (prevTx.getNotes() != null && !prevTx.getNotes().contains(po.getPoNumber())) {
                    throw new InvalidOperationException("Idempotency conflict: Receipt reference '" +
                            request.getReceiptReference() + "' has already been used for another purchase order");
                }
                log.info("Duplicate PO receipt request with reference '{}' detected. Returning current PO state.", request.getReceiptReference());
                return purchaseOrderMapper.toResponse(po);
            }
        }

        User user = getUser(userId);
        Warehouse warehouse = po.getWarehouse();

        for (PurchaseOrderItemReceiveDto recvDto : request.getItems()) {
            PurchaseOrderItem item = purchaseOrderItemRepository.findById(recvDto.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrderItem", "id", recvDto.getItemId()));

            if (!item.getPurchaseOrder().getId().equals(po.getId())) {
                throw new InvalidOperationException("Item " + recvDto.getItemId() + " does not belong to PO " + id);
            }

            int delta = recvDto.getReceivedQuantityDelta();
            if (delta <= 0) {
                continue;
            }

            if (item.getReceivedQuantity() + delta > item.getOrderedQuantity()) {
                throw new InvalidOperationException(String.format(
                        "Cannot receive %d units for SKU '%s'. Ordered: %d, Already Received: %d",
                        delta, item.getProduct().getSku(), item.getOrderedQuantity(), item.getReceivedQuantity()
                ));
            }

            // Update Item
            item.setReceivedQuantity(item.getReceivedQuantity() + delta);
            purchaseOrderItemRepository.save(item);

            // Increment Inventory
            Product product = item.getProduct();
            Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                    .orElseGet(() -> Inventory.builder()
                            .product(product)
                            .warehouse(warehouse)
                            .availableQuantity(0)
                            .reservedQuantity(0)
                            .damagedQuantity(0)
                            .version(0L)
                            .build());

            int prevQty = inventory.getAvailableQuantity();
            int newQty = prevQty + delta;
            inventory.setAvailableQuantity(newQty);
            inventoryRepository.save(inventory);

            // Record STOCK_IN Transaction
            StockTransaction transaction = StockTransaction.builder()
                    .product(product)
                    .warehouse(warehouse)
                    .transactionType(TransactionTypeEnum.STOCK_IN)
                    .quantity(delta)
                    .previousAvailableQuantity(prevQty)
                    .newAvailableQuantity(newQty)
                    .previousReservedQuantity(inventory.getReservedQuantity())
                    .newReservedQuantity(inventory.getReservedQuantity())
                    .previousDamagedQuantity(inventory.getDamagedQuantity())
                    .newDamagedQuantity(inventory.getDamagedQuantity())
                    .reason("Purchase Order Delivery Receipt")
                    .referenceNumber(StringUtils.hasText(request.getReceiptReference()) ? request.getReceiptReference() : po.getPoNumber())
                    .notes("Received line item from PO " + po.getPoNumber())
                    .performedBy(user)
                    .build();
            stockTransactionRepository.save(transaction);
        }

        // Recalculate PO Status
        boolean allReceived = po.getItems().stream().allMatch(i -> i.getReceivedQuantity().equals(i.getOrderedQuantity()));
        boolean anyReceived = po.getItems().stream().anyMatch(i -> i.getReceivedQuantity() > 0);

        if (allReceived) {
            po.setStatus(PurchaseOrderStatusEnum.RECEIVED);
            po.setReceivedDate(LocalDate.now());
        } else if (anyReceived) {
            po.setStatus(PurchaseOrderStatusEnum.PARTIALLY_RECEIVED);
        }

        PurchaseOrder saved = purchaseOrderRepository.save(po);
        auditLogService.logAction(user, "RECEIVE_PO", "PurchaseOrder", saved.getId(),
                "Received items for purchase order " + saved.getPoNumber() +
                (StringUtils.hasText(request.getReceiptReference()) ? " [Ref: " + request.getReceiptReference() + "]" : ""));
        return purchaseOrderMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse cancelPurchaseOrder(Long id, Long userId) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        if (po.getStatus() == PurchaseOrderStatusEnum.RECEIVED) {
            throw new InvalidOperationException("Cannot cancel an already RECEIVED purchase order");
        }

        po.setStatus(PurchaseOrderStatusEnum.CANCELLED);
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        User actor = getUser(userId);
        auditLogService.logAction(actor, "CANCEL_PO", "PurchaseOrder", saved.getId(), "Cancelled purchase order " + saved.getPoNumber());
        return purchaseOrderMapper.toResponse(saved);
    }

    private User getUser(Long id) {
        if (id != null) {
            return userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        }
        return com.stockpilot.security.SecurityUtils.getCurrentUserEmail()
                .flatMap(userRepository::findByEmail)
                .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));
    }
}
