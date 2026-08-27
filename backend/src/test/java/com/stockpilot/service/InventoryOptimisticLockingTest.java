package com.stockpilot.service;

import com.stockpilot.entity.Category;
import com.stockpilot.entity.Inventory;
import com.stockpilot.entity.Product;
import com.stockpilot.entity.Supplier;
import com.stockpilot.entity.Warehouse;
import com.stockpilot.repository.CategoryRepository;
import com.stockpilot.repository.InventoryRepository;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.SupplierRepository;
import com.stockpilot.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("dev")
public class InventoryOptimisticLockingTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    private Product product;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> categoryRepository.save(Category.builder().name("Opt Category").build()));

        Supplier supplier = supplierRepository.findAll().stream().findFirst()
                .orElseGet(() -> supplierRepository.save(Supplier.builder().companyName("Opt Supplier").build()));

        product = productRepository.save(Product.builder()
                .sku("OPT-SKU-" + System.currentTimeMillis())
                .name("Optimistic Lock SKU")
                .category(category)
                .supplier(supplier)
                .costPrice(BigDecimal.valueOf(100))
                .sellingPrice(BigDecimal.valueOf(200))
                .unit("unit")
                .reorderLevel(5)
                .reorderQuantity(10)
                .active(true)
                .build());

        warehouse = warehouseRepository.findAll().stream().findFirst()
                .orElseGet(() -> warehouseRepository.save(Warehouse.builder().name("Opt WH").code("OPT-WH").build()));
    }

    @Test
    @DisplayName("Concurrent update with stale version throws ObjectOptimisticLockingFailureException")
    void testOptimisticLockingConflict() {
        // Create initial inventory row
        Inventory initial = Inventory.builder()
                .product(product)
                .warehouse(warehouse)
                .availableQuantity(50)
                .reservedQuantity(0)
                .damagedQuantity(0)
                .build();
        Inventory saved = inventoryRepository.saveAndFlush(initial);

        // Fetch two instances representing two concurrent transactions
        Inventory tx1Inventory = inventoryRepository.findById(saved.getId()).orElseThrow();
        Inventory tx2Inventory = inventoryRepository.findById(saved.getId()).orElseThrow();

        // Transaction 1 updates and commits successfully
        tx1Inventory.setAvailableQuantity(40);
        inventoryRepository.saveAndFlush(tx1Inventory);

        // Transaction 2 tries to update with stale version
        tx2Inventory.setAvailableQuantity(35);

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            inventoryRepository.saveAndFlush(tx2Inventory);
        });
    }
}
