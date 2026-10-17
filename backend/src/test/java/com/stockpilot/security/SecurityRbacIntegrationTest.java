package com.stockpilot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpilot.dto.request.StockAdjustmentRequest;
import com.stockpilot.entity.Product;
import com.stockpilot.entity.Warehouse;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
public class SecurityRbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    private Product testProduct;
    private Warehouse testWarehouse;

    @BeforeEach
    void setUp() {
        testProduct = productRepository.findAll().stream().findFirst().orElseThrow();
        testWarehouse = warehouseRepository.findAll().stream().findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint returns HTTP 401 Unauthorized")
    void testUnauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(username = "staff@stockpilot.com", roles = {"STAFF"})
    @DisplayName("STAFF is forbidden (HTTP 403) from approving purchase orders")
    void testStaffCannotApprovePurchaseOrder() throws Exception {
        mockMvc.perform(post("/api/purchase-orders/1/approve"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(username = "staff@stockpilot.com", roles = {"STAFF"})
    @DisplayName("STAFF is forbidden (HTTP 403) from arbitrary stock adjustments")
    void testStaffCannotAdjustStock() throws Exception {
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .productId(testProduct.getId())
                .warehouseId(testWarehouse.getId())
                .resultingQuantity(999)
                .reason("Unauthorized staff adjustment attempt")
                .build();

        mockMvc.perform(post("/api/inventory/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "staff@stockpilot.com", roles = {"STAFF"})
    @DisplayName("STAFF is forbidden (HTTP 403) from user administration")
    void testStaffCannotAccessUserAdmin() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager@stockpilot.com", roles = {"INVENTORY_MANAGER"})
    @DisplayName("INVENTORY_MANAGER is forbidden (HTTP 403) from user administration")
    void testManagerCannotAccessUserAdmin() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@stockpilot.com", roles = {"ADMIN"})
    @DisplayName("ADMIN can access user administration (HTTP 200)")
    void testAdminCanAccessUserAdmin() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "manager@stockpilot.com", roles = {"INVENTORY_MANAGER"})
    @DisplayName("INVENTORY_MANAGER is authorized to access system audit logs (HTTP 200)")
    void testManagerCanAccessAuditLogs() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "staff@stockpilot.com", roles = {"STAFF"})
    @DisplayName("STAFF is forbidden (HTTP 403) from accessing system audit logs")
    void testStaffCannotAccessAuditLogs() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Public actuator health endpoint is accessible without authentication (HTTP 200)")
    void testActuatorHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
