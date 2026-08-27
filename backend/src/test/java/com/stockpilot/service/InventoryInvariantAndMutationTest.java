package com.stockpilot.service;

import com.stockpilot.dto.request.*;
import com.stockpilot.dto.response.InventoryResponse;
import com.stockpilot.entity.*;
import com.stockpilot.exception.InsufficientStockException;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
public class InventoryInvariantAndMutationTest {

    @Autowired
    private InventoryService inventoryService;

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
    private ReservationRepository reservationRepository;

    private Product testProduct;
    private Warehouse warehouseA;
    private Warehouse warehouseB;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> categoryRepository.save(Category.builder().name("Test Cat " + System.currentTimeMillis()).build()));

        Supplier supplier = supplierRepository.findAll().stream().findFirst()
                .orElseGet(() -> supplierRepository.save(Supplier.builder().companyName("Test Supp " + System.currentTimeMillis()).build()));

        testProduct = productRepository.save(Product.builder()
                .sku("TEST-SKU-" + System.currentTimeMillis())
                .name("Enterprise Test Laptop")
                .category(category)
                .supplier(supplier)
                .costPrice(BigDecimal.valueOf(1000))
                .sellingPrice(BigDecimal.valueOf(1500))
                .unit("unit")
                .reorderLevel(10)
                .reorderQuantity(20)
                .active(true)
                .build());

        List<Warehouse> warehouses = warehouseRepository.findAll();
        if (warehouses.size() >= 2) {
            warehouseA = warehouses.get(0);
            warehouseB = warehouses.get(1);
        } else {
            warehouseA = warehouseRepository.save(Warehouse.builder().name("Hub A").code("WH-A-" + System.currentTimeMillis()).build());
            warehouseB = warehouseRepository.save(Warehouse.builder().name("Hub B").code("WH-B-" + System.currentTimeMillis()).build());
        }
    }

    private void assertInvariant(Inventory inv) {
        assertNotNull(inv);
        int avail = inv.getAvailableQuantity() != null ? inv.getAvailableQuantity() : 0;
        int res = inv.getReservedQuantity() != null ? inv.getReservedQuantity() : 0;
        int dam = inv.getDamagedQuantity() != null ? inv.getDamagedQuantity() : 0;
        int onHand = inv.getOnHandQuantity();

        assertTrue(avail >= 0, "Available quantity must be non-negative: " + avail);
        assertTrue(res >= 0, "Reserved quantity must be non-negative: " + res);
        assertTrue(dam >= 0, "Damaged quantity must be non-negative: " + dam);
        assertEquals(avail + res + dam, onHand, "Invariant violated: onHand != available + reserved + damaged");
    }

    @Test
    @DisplayName("STOCK_IN increases available stock and preserves invariant")
    void testStockIn() {
        StockInRequest request = StockInRequest.builder()
                .productId(testProduct.getId())
                .warehouseId(warehouseA.getId())
                .quantity(50)
                .reason("Batch Arrival")
                .build();

        InventoryResponse response = inventoryService.stockIn(request, 1L);
        assertEquals(50, response.getAvailableQuantity());
        assertEquals(0, response.getReservedQuantity());
        assertEquals(0, response.getDamagedQuantity());
        assertEquals(50, response.getOnHandQuantity());

        Inventory saved = inventoryRepository.findByProductIdAndWarehouseId(testProduct.getId(), warehouseA.getId()).orElseThrow();
        assertInvariant(saved);
    }

    @Test
    @DisplayName("STOCK_OUT decreases available stock and throws InsufficientStockException on deficit")
    void testStockOutAndInsufficientStock() {
        // Initial Stock In
        inventoryService.stockIn(StockInRequest.builder()
                .productId(testProduct.getId())
                .warehouseId(warehouseA.getId())
                .quantity(30)
                .build(), 1L);

        // Valid Stock Out
        InventoryResponse response = inventoryService.stockOut(StockOutRequest.builder()
                .productId(testProduct.getId())
                .warehouseId(warehouseA.getId())
                .quantity(10)
                .reason("Order Dispatch")
                .build(), 1L);

        assertEquals(20, response.getAvailableQuantity());
        assertEquals(20, response.getOnHandQuantity());

        // Stock Out Exceeding Available
        assertThrows(InsufficientStockException.class, () -> {
            inventoryService.stockOut(StockOutRequest.builder()
                    .productId(testProduct.getId())
                    .warehouseId(warehouseA.getId())
                    .quantity(25)
                    .build(), 1L);
        });

        Inventory saved = inventoryRepository.findByProductIdAndWarehouseId(testProduct.getId(), warehouseA.getId()).orElseThrow();
        assertInvariant(saved);
    }

    @Test
    @DisplayName("RESERVE shifts stock from available to reserved while onHand remains constant")
    void testReservationLifecycle() {
        inventoryService.stockIn(StockInRequest.builder()
                .productId(testProduct.getId())
                .warehouseId(warehouseA.getId())
                .quantity(40)
                .build(), 1L);

        // Reserve 15 units
        InventoryResponse reservedRes = inventoryService.reserveStock(StockReserveRequest.builder()
                .productId(testProduct.getId())
                .warehouseId(warehouseA.getId())
                .quantity(15)
                .referenceNumber("ORD-RES-01")
                .build(), 1L);

        assertEquals(25, reservedRes.getAvailableQuantity());
        assertEquals(15, reservedRes.getReservedQuantity());
        assertEquals(40, reservedRes.getOnHandQuantity()); // onHand unchanged!

        Inventory saved = inventoryRepository.findByProductIdAndWarehouseId(testProduct.getId(), warehouseA.getId()).orElseThrow();
        assertInvariant(saved);

        // Over-reservation rejection
        assertThrows(InsufficientStockException.class, () -> {
            inventoryService.reserveStock(StockReserveRequest.builder()
                    .productId(testProduct.getId())
                    .warehouseId(warehouseA.getId())
                    .quantity(30)
                    .referenceNumber("ORD-RES-FAIL")
                    .build(), 1L);
        });

        // Release Reservation
        Reservation activeRes = reservationRepository.findAll().stream()
                .filter(r -> "ORD-RES-01".equals(r.getReferenceNumber()))
                .findFirst().orElseThrow();

        InventoryResponse releasedRes = inventoryService.releaseStock(StockReleaseRequest.builder()
                .reservationId(activeRes.getId())
                .quantity(15)
                .build(), 1L);

        assertEquals(40, releasedRes.getAvailableQuantity());
        assertEquals(0, releasedRes.getReservedQuantity());
        assertEquals(40, releasedRes.getOnHandQuantity());

        Inventory savedAfterRelease = inventoryRepository.findByProductIdAndWarehouseId(testProduct.getId(), warehouseA.getId()).orElseThrow();
        assertInvariant(savedAfterRelease);
    }

    @Test
    @DisplayName("DAMAGE shifts available stock to damaged while onHand remains constant")
    void testDamageStock() {
        inventoryService.stockIn(StockInRequest.builder()
                .productId(testProduct.getId())
                .warehouseId(warehouseA.getId())
                .quantity(20)
                .build(), 1L);

        InventoryResponse response = inventoryService.damageStock(StockDamageRequest.builder()
                .productId(testProduct.getId())
                .warehouseId(warehouseA.getId())
                .quantity(4)
                .reason("Dropped during forklift transit")
                .build(), 1L);

        assertEquals(16, response.getAvailableQuantity());
        assertEquals(4, response.getDamagedQuantity());
        assertEquals(20, response.getOnHandQuantity());

        Inventory saved = inventoryRepository.findByProductIdAndWarehouseId(testProduct.getId(), warehouseA.getId()).orElseThrow();
        assertInvariant(saved);
    }

    @Test
    @DisplayName("RETURN increases available stock")
    void testReturnStock() {
        inventoryService.stockIn(StockInRequest.builder()
                .productId(testProduct.getId())
                .warehouseId(warehouseA.getId())
                .quantity(10)
                .build(), 1L);

        InventoryResponse response = inventoryService.returnStock(StockReturnRequest.builder()
                .productId(testProduct.getId())
                .warehouseId(warehouseA.getId())
                .quantity(5)
                .reason("Customer return in sealed box")
                .build(), 1L);

        assertEquals(15, response.getAvailableQuantity());
        assertEquals(15, response.getOnHandQuantity());

        Inventory saved = inventoryRepository.findByProductIdAndWarehouseId(testProduct.getId(), warehouseA.getId()).orElseThrow();
        assertInvariant(saved);
    }

    @Test
    @DisplayName("TRANSFER atomically shifts stock between warehouses preserving total system inventory")
    void testWarehouseTransfer() {
        inventoryService.stockIn(StockInRequest.builder()
                .productId(testProduct.getId())
                .warehouseId(warehouseA.getId())
                .quantity(30)
                .build(), 1L);

        List<InventoryResponse> transfers = inventoryService.transferStock(StockTransferRequest.builder()
                .productId(testProduct.getId())
                .sourceWarehouseId(warehouseA.getId())
                .destinationWarehouseId(warehouseB.getId())
                .quantity(12)
                .referenceNumber("TR-001")
                .build(), 1L);

        assertEquals(2, transfers.size());
        assertEquals(18, transfers.get(0).getAvailableQuantity()); // Source (A)
        assertEquals(12, transfers.get(1).getAvailableQuantity()); // Destination (B)

        Inventory invA = inventoryRepository.findByProductIdAndWarehouseId(testProduct.getId(), warehouseA.getId()).orElseThrow();
        Inventory invB = inventoryRepository.findByProductIdAndWarehouseId(testProduct.getId(), warehouseB.getId()).orElseThrow();

        assertInvariant(invA);
        assertInvariant(invB);
        assertEquals(30, invA.getOnHandQuantity() + invB.getOnHandQuantity());
    }

    @Test
    @DisplayName("Transfer to same warehouse is rejected")
    void testTransferToSameWarehouse() {
        assertThrows(InvalidOperationException.class, () -> {
            inventoryService.transferStock(StockTransferRequest.builder()
                    .productId(testProduct.getId())
                    .sourceWarehouseId(warehouseA.getId())
                    .destinationWarehouseId(warehouseA.getId())
                    .quantity(5)
                    .build(), 1L);
        });
    }
}
