package com.stockpilot;

import com.stockpilot.entity.*;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HibernateSchemaMappingTest {

    @Test
    @DisplayName("Verify enum-backed entity columns map to VARCHAR in MySQLDialect")
    void testEnumColumnsAreMappedAsVarcharInMySQLDialect() {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.MySQLDialect")
                .build();
        try {
            MetadataSources sources = new MetadataSources(registry);
            sources.addAnnotatedClass(PurchaseOrder.class);
            sources.addAnnotatedClass(PurchaseOrderItem.class);
            sources.addAnnotatedClass(User.class);
            sources.addAnnotatedClass(Reservation.class);
            sources.addAnnotatedClass(StockTransaction.class);
            sources.addAnnotatedClass(Product.class);
            sources.addAnnotatedClass(Category.class);
            sources.addAnnotatedClass(Supplier.class);
            sources.addAnnotatedClass(Warehouse.class);
            sources.addAnnotatedClass(AuditLog.class);

            Metadata metadata = sources.buildMetadata();

            assertColumnSqlTypeCode(metadata, "purchase_orders", "status", SqlTypes.VARCHAR);
            assertColumnSqlTypeCode(metadata, "users", "role", SqlTypes.VARCHAR);
            assertColumnSqlTypeCode(metadata, "reservations", "status", SqlTypes.VARCHAR);
            assertColumnSqlTypeCode(metadata, "stock_transactions", "transaction_type", SqlTypes.VARCHAR);
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    private void assertColumnSqlTypeCode(Metadata metadata, String tableName, String columnName, int expectedSqlTypeCode) {
        Table table = metadata.getDatabase().getDefaultNamespace().locateTable(metadata.getDatabase().toIdentifier(tableName));
        assertNotNull(table, "Table " + tableName + " must exist in Hibernate metadata");
        Column column = table.getColumn(metadata.getDatabase().toIdentifier(columnName));
        assertNotNull(column, "Column " + columnName + " must exist in table " + tableName);
        Integer actualSqlTypeCode = column.getSqlTypeCode(metadata);
        assertEquals(expectedSqlTypeCode, actualSqlTypeCode != null ? actualSqlTypeCode : column.getSqlTypeCode(),
                "Column " + columnName + " on " + tableName + " should have SQL type code " + expectedSqlTypeCode);
    }
}
