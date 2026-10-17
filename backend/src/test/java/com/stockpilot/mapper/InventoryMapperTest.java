package com.stockpilot.mapper;

import com.stockpilot.dto.response.InventoryResponse;
import com.stockpilot.entity.Inventory;
import com.stockpilot.entity.Product;
import com.stockpilot.entity.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventoryMapperTest {

    private InventoryMapper inventoryMapper;

    @BeforeEach
    void setUp() {
        inventoryMapper = new InventoryMapper();
    }

    @Test
    @DisplayName("Maps full inventory with valid product and warehouse correctly")
    void testToResponse_withValidProductAndWarehouse() {
        Product product = Product.builder()
                .id(101L)
                .sku("PROD-TEST-001")
                .name("High-End Laptop")
                .unit("unit")
                .reorderLevel(15)
                .build();

        Warehouse warehouse = Warehouse.builder()
                .id(201L)
                .name("Main Distribution Center")
                .code("MDC-01")
                .build();

        Inventory inventory = Inventory.builder()
                .id(1L)
                .product(product)
                .warehouse(warehouse)
                .availableQuantity(25)
                .reservedQuantity(5)
                .damagedQuantity(2)
                .version(1L)
                .build();

        InventoryResponse response = inventoryMapper.toResponse(inventory, "LOW");

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(101L, response.getProductId());
        assertEquals("PROD-TEST-001", response.getProductSku());
        assertEquals("High-End Laptop", response.getProductName());
        assertEquals("unit", response.getProductUnit());
        assertEquals(15, response.getReorderLevel());
        assertEquals(201L, response.getWarehouseId());
        assertEquals("Main Distribution Center", response.getWarehouseName());
        assertEquals("MDC-01", response.getWarehouseCode());
        assertEquals(25, response.getAvailableQuantity());
        assertEquals(5, response.getReservedQuantity());
        assertEquals(2, response.getDamagedQuantity());
        assertEquals(32, response.getOnHandQuantity());
        assertEquals("HEALTHY", response.getStockStatus());
        assertEquals("LOW", response.getStockoutRisk());
        assertEquals(1L, response.getVersion());
    }

    @Test
    @DisplayName("Correctly detects LOW_STOCK when available is at or below reorderLevel")
    void testToResponse_lowStockCondition() {
        Product product = Product.builder()
                .id(102L)
                .reorderLevel(15)
                .build();

        Inventory inventory = Inventory.builder()
                .id(2L)
                .product(product)
                .availableQuantity(10)
                .reservedQuantity(0)
                .damagedQuantity(0)
                .build();

        InventoryResponse response = inventoryMapper.toResponse(inventory, "HIGH");

        assertNotNull(response);
        assertEquals("LOW_STOCK", response.getStockStatus());
        assertEquals(15, response.getReorderLevel());
    }

    @Test
    @DisplayName("Correctly detects OUT_OF_STOCK when availableQuantity is 0")
    void testToResponse_outOfStockCondition() {
        Product product = Product.builder()
                .id(103L)
                .reorderLevel(15)
                .build();

        Inventory inventory = Inventory.builder()
                .id(3L)
                .product(product)
                .availableQuantity(0)
                .reservedQuantity(3)
                .damagedQuantity(0)
                .build();

        InventoryResponse response = inventoryMapper.toResponse(inventory, "CRITICAL");

        assertNotNull(response);
        assertEquals("OUT_OF_STOCK", response.getStockStatus());
    }

    @Test
    @DisplayName("Preserves genuine reorderLevel of zero without conflating with missing data")
    void testToResponse_withGenuineReorderLevelZero() {
        Product product = Product.builder()
                .id(104L)
                .reorderLevel(0)
                .build();

        Inventory inventory = Inventory.builder()
                .id(4L)
                .product(product)
                .availableQuantity(5)
                .reservedQuantity(0)
                .damagedQuantity(0)
                .build();

        InventoryResponse response = inventoryMapper.toResponse(inventory, "LOW");

        assertNotNull(response);
        assertEquals(0, response.getReorderLevel());
        assertEquals("HEALTHY", response.getStockStatus());
    }

    @Test
    @DisplayName("Safely handles inventory record with missing product and warehouse without crashing or fabricating reorderLevel")
    void testToResponse_withMissingProductAndWarehouse() {
        Inventory inventory = Inventory.builder()
                .id(5L)
                .product(null)
                .warehouse(null)
                .availableQuantity(12)
                .reservedQuantity(2)
                .damagedQuantity(0)
                .build();

        InventoryResponse response = inventoryMapper.toResponse(inventory, "LOW");

        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertNull(response.getProductId());
        assertNull(response.getProductSku());
        assertNull(response.getProductName());
        assertNull(response.getProductUnit());
        assertNull(response.getReorderLevel(), "Reorder level must be null when product is missing, not forged as default");
        assertNull(response.getWarehouseId());
        assertNull(response.getWarehouseName());
        assertNull(response.getWarehouseCode());
        assertEquals(12, response.getAvailableQuantity());
        assertEquals(14, response.getOnHandQuantity());
        assertEquals("HEALTHY", response.getStockStatus());
    }

    @Test
    @DisplayName("Returns null when input inventory entity is null")
    void testToResponse_nullInventoryReturnsNull() {
        assertNull(inventoryMapper.toResponse(null, "LOW"));
    }
}
