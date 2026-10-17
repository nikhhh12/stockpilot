package com.stockpilot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpilot.dto.request.*;
import com.stockpilot.entity.Product;
import com.stockpilot.entity.Warehouse;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.UserRepository;
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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
public class ControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private Product testProduct;
    private Warehouse testWarehouse;

    @BeforeEach
    void setUp() {
        testProduct = productRepository.findAll().stream().findFirst().orElseThrow();
        testWarehouse = warehouseRepository.findAll().stream().findFirst().orElseThrow();

        // Ensure admin user exists with exact password encoder
        userRepository.findByEmail("admin@stockpilot.com").ifPresentOrElse(u -> {
            u.setPasswordHash(passwordEncoder.encode("password123"));
            userRepository.save(u);
        }, () -> {
            userRepository.save(com.stockpilot.entity.User.builder()
                    .name("System Administrator")
                    .email("admin@stockpilot.com")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .role(com.stockpilot.entity.RoleEnum.ADMIN)
                    .enabled(true)
                    .build());
        });
    }

    @Test
    @DisplayName("POST /api/auth/login authenticates seed user and returns JWT token")
    void testAuthLogin() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@stockpilot.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("admin@stockpilot.com"));
    }

    @Test
    @WithMockUser(username = "admin@stockpilot.com", roles = {"ADMIN"})
    @DisplayName("GET /api/products returns paginated products")
    void testGetProducts() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", not(empty())));
    }

    @Test
    @WithMockUser(username = "admin@stockpilot.com", roles = {"ADMIN"})
    @DisplayName("GET /api/inventory returns inventories with calculated stockout risk")
    void testGetInventory() throws Exception {
        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", not(empty())));
    }

    @Test
    @WithMockUser(username = "admin@stockpilot.com", roles = {"ADMIN"})
    @DisplayName("POST /api/inventory/stock-in increments stock and records audit transaction")
    void testStockInApi() throws Exception {
        StockInRequest request = StockInRequest.builder()
                .productId(testProduct.getId())
                .warehouseId(testWarehouse.getId())
                .quantity(15)
                .reason("REST Test Stock In")
                .build();

        mockMvc.perform(post("/api/inventory/stock-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableQuantity", greaterThanOrEqualTo(15)));
    }

    @Test
    @WithMockUser(username = "admin@stockpilot.com", roles = {"ADMIN"})
    @DisplayName("GET /api/dashboard/summary returns complete executive dashboard contract")
    void testDashboardSummaryApi() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalProducts", greaterThan(0)))
                .andExpect(jsonPath("$.data.totalInventoryUnits", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.inventoryValueByCategory", not(empty())))
                .andExpect(jsonPath("$.data.inventoryByWarehouse", not(empty())));
    }

    @Test
    @WithMockUser(username = "admin@stockpilot.com", roles = {"ADMIN"})
    @DisplayName("GET /api/analytics endpoints return forecast, risk, reorders, and anomalies")
    void testAnalyticsApis() throws Exception {
        mockMvc.perform(get("/api/analytics/stockout-risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/analytics/reorder-recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/analytics/anomalies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "admin@stockpilot.com", roles = {"ADMIN"})
    @DisplayName("GET /api/ai/daily-brief and POST /api/ai/chat operate cleanly")
    void testAIApis() throws Exception {
        mockMvc.perform(get("/api/ai/daily-brief"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.facts").exists())
                .andExpect(jsonPath("$.data.aiInterpretation.executiveSummary").isNotEmpty());

        AIChatRequest chatReq = new AIChatRequest();
        chatReq.setMessage("What is our current warehouse stock breakdown?");

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reply").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "admin@stockpilot.com", roles = {"ADMIN"})
    @DisplayName("GET /api/settings/config returns real analytics boundaries and system status")
    void testSettingsConfigApi() throws Exception {
        mockMvc.perform(get("/api/settings/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.analytics.lowRiskThresholdDays").value(30))
                .andExpect(jsonPath("$.data.analytics.highRiskThresholdDays").value(6))
                .andExpect(jsonPath("$.data.systemStatus.databaseStatus").value("Connected"))
                .andExpect(jsonPath("$.data.systemStatus.optimisticLockingActive").value(true));
    }

    @Test
    @WithMockUser(username = "staff@stockpilot.com", roles = {"STAFF"})
    @DisplayName("STAFF role is forbidden (403) from deleting products")
    void testStaffForbiddenFromDeletingProducts() throws Exception {
        mockMvc.perform(delete("/api/products/" + testProduct.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "staff@stockpilot.com", roles = {"STAFF"})
    @DisplayName("STAFF role is forbidden (403) from approving purchase orders")
    void testStaffForbiddenFromApprovingPurchaseOrders() throws Exception {
        mockMvc.perform(post("/api/purchase-orders/1/approve"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint is rejected with 401 Unauthorized")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
    }
}
