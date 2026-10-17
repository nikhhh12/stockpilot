package com.stockpilot.service;

import com.stockpilot.dto.request.PurchaseOrderCreateRequest;
import com.stockpilot.dto.request.PurchaseOrderItemReceiveDto;
import com.stockpilot.dto.request.PurchaseOrderItemRequest;
import com.stockpilot.dto.request.PurchaseOrderReceiveRequest;
import com.stockpilot.dto.response.PurchaseOrderResponse;
import com.stockpilot.entity.*;
import com.stockpilot.exception.InvalidOperationException;
import com.stockpilot.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
public class PurchaseOrderLifecycleTest {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

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
    private AuditLogRepository auditLogRepository;

    private Product product;
    private Supplier supplier;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> categoryRepository.save(Category.builder().name("PO Category").build()));

        supplier = supplierRepository.findAll().stream().findFirst()
                .orElseGet(() -> supplierRepository.save(Supplier.builder().companyName("PO Supplier").build()));

        product = productRepository.save(Product.builder()
                .sku("PO-TEST-SKU-" + System.currentTimeMillis())
                .name("PO Test Product")
                .category(category)
                .supplier(supplier)
                .costPrice(BigDecimal.valueOf(250))
                .sellingPrice(BigDecimal.valueOf(400))
                .unit("unit")
                .reorderLevel(10)
                .reorderQuantity(50)
                .active(true)
                .build());

        warehouse = warehouseRepository.findAll().stream().findFirst()
                .orElseGet(() -> warehouseRepository.save(Warehouse.builder().name("PO WH").code("PO-WH").build()));
    }

    @Test
    @DisplayName("Complete PO lifecycle: DRAFT -> PENDING -> APPROVED -> ORDERED -> PARTIALLY_RECEIVED -> RECEIVED")
    void testPurchaseOrderFullLifecycle() {
        // 1. Create Purchase Order (Starts in PENDING)
        PurchaseOrderCreateRequest createReq = PurchaseOrderCreateRequest.builder()
                .supplierId(supplier.getId())
                .warehouseId(warehouse.getId())
                .expectedDeliveryDate(LocalDate.now().plusDays(5))
                .notes("Test Lifecycle PO")
                .items(List.of(
                        PurchaseOrderItemRequest.builder()
                                .productId(product.getId())
                                .orderedQuantity(50)
                                .unitCost(BigDecimal.valueOf(250))
                                .build()
                ))
                .build();

        PurchaseOrderResponse created = purchaseOrderService.createPurchaseOrder(createReq, 1L);
        assertNotNull(created.getId());
        assertEquals(PurchaseOrderStatusEnum.PENDING, created.getStatus());
        assertEquals(50, created.getItems().get(0).getOrderedQuantity());
        assertEquals(0, created.getItems().get(0).getReceivedQuantity());

        // Cannot transition directly from PENDING to ORDERED
        assertThrows(InvalidOperationException.class, () -> {
            purchaseOrderService.orderPurchaseOrder(created.getId(), 1L);
        });

        // 2. Approve PO
        PurchaseOrderResponse approved = purchaseOrderService.approvePurchaseOrder(created.getId(), 1L);
        assertEquals(PurchaseOrderStatusEnum.APPROVED, approved.getStatus());

        // 3. Place Order (ORDERED)
        PurchaseOrderResponse ordered = purchaseOrderService.orderPurchaseOrder(approved.getId(), 1L);
        assertEquals(PurchaseOrderStatusEnum.ORDERED, ordered.getStatus());

        // Initial Available Inventory before delivery
        int initialAvail = inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .map(Inventory::getAvailableQuantity).orElse(0);

        // 4. Partial Delivery: Receive 20 units
        Long itemId = ordered.getItems().get(0).getId();
        PurchaseOrderReceiveRequest partialReq = PurchaseOrderReceiveRequest.builder()
                .items(List.of(
                        PurchaseOrderItemReceiveDto.builder()
                                .itemId(itemId)
                                .receivedQuantityDelta(20)
                                .build()
                ))
                .build();

        PurchaseOrderResponse partialReceived = purchaseOrderService.receivePurchaseOrder(ordered.getId(), partialReq, 1L);
        assertEquals(PurchaseOrderStatusEnum.PARTIALLY_RECEIVED, partialReceived.getStatus());
        assertEquals(20, partialReceived.getItems().get(0).getReceivedQuantity());

        // Verify inventory increased by 20
        Inventory invAfterPartial = inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId()).orElseThrow();
        assertEquals(initialAvail + 20, invAfterPartial.getAvailableQuantity());

        // 5. Over-receiving rejection (Ordered: 50, Received: 20, Trying to receive 35 -> 55 > 50)
        assertThrows(InvalidOperationException.class, () -> {
            purchaseOrderService.receivePurchaseOrder(ordered.getId(), PurchaseOrderReceiveRequest.builder()
                    .items(List.of(
                            PurchaseOrderItemReceiveDto.builder()
                                    .itemId(itemId)
                                    .receivedQuantityDelta(35)
                                    .build()
                    ))
                    .build(), 1L);
        });

        // 6. Remaining Delivery: Receive remaining 30 units
        PurchaseOrderReceiveRequest finalReq = PurchaseOrderReceiveRequest.builder()
                .items(List.of(
                        PurchaseOrderItemReceiveDto.builder()
                                .itemId(itemId)
                                .receivedQuantityDelta(30)
                                .build()
                ))
                .build();

        PurchaseOrderResponse fullyReceived = purchaseOrderService.receivePurchaseOrder(ordered.getId(), finalReq, 1L);
        assertEquals(PurchaseOrderStatusEnum.RECEIVED, fullyReceived.getStatus());
        assertEquals(50, fullyReceived.getItems().get(0).getReceivedQuantity());

        // Verify final inventory increased by total 50
        Inventory invFinal = inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId()).orElseThrow();
        assertEquals(initialAvail + 50, invFinal.getAvailableQuantity());

        // 7. Cannot cancel already RECEIVED PO
        assertThrows(InvalidOperationException.class, () -> {
            purchaseOrderService.cancelPurchaseOrder(fullyReceived.getId(), 1L);
        });
    }

    @Test
    @DisplayName("PO Cancellation updates status to CANCELLED")
    void testCancelPurchaseOrder() {
        PurchaseOrderCreateRequest createReq = PurchaseOrderCreateRequest.builder()
                .supplierId(supplier.getId())
                .warehouseId(warehouse.getId())
                .expectedDeliveryDate(LocalDate.now().plusDays(5))
                .items(List.of(
                        PurchaseOrderItemRequest.builder()
                                .productId(product.getId())
                                .orderedQuantity(10)
                                .unitCost(BigDecimal.valueOf(100))
                                .build()
                ))
                .build();

        PurchaseOrderResponse created = purchaseOrderService.createPurchaseOrder(createReq, 1L);
        PurchaseOrderResponse cancelled = purchaseOrderService.cancelPurchaseOrder(created.getId(), 1L);
        assertEquals(PurchaseOrderStatusEnum.CANCELLED, cancelled.getStatus());
    }

    @Test
    @DisplayName("Idempotent receiving with receiptReference does not double-receive units")
    void testIdempotentReceiveDoesNotDoubleReceive() {
        PurchaseOrderCreateRequest createReq = PurchaseOrderCreateRequest.builder()
                .supplierId(supplier.getId())
                .warehouseId(warehouse.getId())
                .expectedDeliveryDate(LocalDate.now().plusDays(5))
                .items(List.of(
                        PurchaseOrderItemRequest.builder()
                                .productId(product.getId())
                                .orderedQuantity(20)
                                .unitCost(BigDecimal.valueOf(100))
                                .build()
                ))
                .build();

        PurchaseOrderResponse created = purchaseOrderService.createPurchaseOrder(createReq, 1L);
        purchaseOrderService.approvePurchaseOrder(created.getId(), 1L);
        purchaseOrderService.orderPurchaseOrder(created.getId(), 1L);

        Long itemId = created.getItems().get(0).getId();

        PurchaseOrderReceiveRequest receiveReq = PurchaseOrderReceiveRequest.builder()
                .receiptReference("RCV-REF-12345")
                .items(List.of(
                        PurchaseOrderItemReceiveDto.builder()
                                .itemId(itemId)
                                .receivedQuantityDelta(10)
                                .build()
                ))
                .build();

        PurchaseOrderResponse firstReceive = purchaseOrderService.receivePurchaseOrder(created.getId(), receiveReq, 1L);
        assertEquals(10, firstReceive.getItems().get(0).getReceivedQuantity());

        // Re-sending same request with same receiptReference
        PurchaseOrderResponse secondReceive = purchaseOrderService.receivePurchaseOrder(created.getId(), receiveReq, 1L);
        assertEquals(10, secondReceive.getItems().get(0).getReceivedQuantity());
    }

    @Test
    @DisplayName("Conflicting reuse of receiptReference on a different purchase order is rejected")
    void testConflictingReceiptReferenceRejection() {
        PurchaseOrderCreateRequest createReq1 = PurchaseOrderCreateRequest.builder()
                .supplierId(supplier.getId())
                .warehouseId(warehouse.getId())
                .expectedDeliveryDate(LocalDate.now().plusDays(5))
                .items(List.of(
                        PurchaseOrderItemRequest.builder()
                                .productId(product.getId())
                                .orderedQuantity(20)
                                .unitCost(BigDecimal.valueOf(100))
                                .build()
                ))
                .build();
        PurchaseOrderResponse po1 = purchaseOrderService.createPurchaseOrder(createReq1, 1L);
        purchaseOrderService.approvePurchaseOrder(po1.getId(), 1L);
        purchaseOrderService.orderPurchaseOrder(po1.getId(), 1L);

        // Receive PO 1 with specific receipt reference
        purchaseOrderService.receivePurchaseOrder(po1.getId(), PurchaseOrderReceiveRequest.builder()
                .receiptReference("RCV-UNIQUE-999")
                .items(List.of(
                        PurchaseOrderItemReceiveDto.builder()
                                .itemId(po1.getItems().get(0).getId())
                                .receivedQuantityDelta(5)
                                .build()
                ))
                .build(), 1L);

        // Create a distinct PO 2
        PurchaseOrderCreateRequest createReq2 = PurchaseOrderCreateRequest.builder()
                .supplierId(supplier.getId())
                .warehouseId(warehouse.getId())
                .expectedDeliveryDate(LocalDate.now().plusDays(5))
                .items(List.of(
                        PurchaseOrderItemRequest.builder()
                                .productId(product.getId())
                                .orderedQuantity(15)
                                .unitCost(BigDecimal.valueOf(100))
                                .build()
                ))
                .build();
        PurchaseOrderResponse po2 = purchaseOrderService.createPurchaseOrder(createReq2, 1L);
        purchaseOrderService.approvePurchaseOrder(po2.getId(), 1L);
        purchaseOrderService.orderPurchaseOrder(po2.getId(), 1L);

        // Attempting to reuse same receiptReference for PO 2 must fail with conflict exception
        assertThrows(InvalidOperationException.class, () -> {
            purchaseOrderService.receivePurchaseOrder(po2.getId(), PurchaseOrderReceiveRequest.builder()
                    .receiptReference("RCV-UNIQUE-999")
                    .items(List.of(
                            PurchaseOrderItemReceiveDto.builder()
                                    .itemId(po2.getItems().get(0).getId())
                                    .receivedQuantityDelta(5)
                                    .build()
                    ))
                    .build(), 1L);
        });
    }

    @Test
    @DisplayName("PO lifecycle transitions write immutable AuditLog records")
    void testPurchaseOrderWritesAuditLogs() {
        PurchaseOrderCreateRequest createReq = PurchaseOrderCreateRequest.builder()
                .supplierId(supplier.getId())
                .warehouseId(warehouse.getId())
                .expectedDeliveryDate(LocalDate.now().plusDays(5))
                .items(List.of(
                        PurchaseOrderItemRequest.builder()
                                .productId(product.getId())
                                .orderedQuantity(10)
                                .unitCost(BigDecimal.valueOf(100))
                                .build()
                ))
                .build();
        PurchaseOrderResponse po = purchaseOrderService.createPurchaseOrder(createReq, 1L);
        purchaseOrderService.approvePurchaseOrder(po.getId(), 1L);
        purchaseOrderService.orderPurchaseOrder(po.getId(), 1L);

        List<AuditLog> logs = auditLogRepository.findAll();
        boolean hasApproveLog = logs.stream().anyMatch(l -> "APPROVE_PO".equals(l.getAction()) && po.getId().equals(l.getEntityId()));
        boolean hasOrderLog = logs.stream().anyMatch(l -> "ORDER_PO".equals(l.getAction()) && po.getId().equals(l.getEntityId()));

        assertTrue(hasApproveLog, "Audit log for APPROVE_PO must be recorded");
        assertTrue(hasOrderLog, "Audit log for ORDER_PO must be recorded");
    }
}
