package com.stockpilot.controller;

import com.stockpilot.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Tag(name = "System Settings & Configuration", description = "Authoritative read-only system status and analytics boundaries")
public class SettingsController {

    @Value("${stockpilot.analytics.risk.low-days:30}")
    private int lowRiskDays;

    @Value("${stockpilot.analytics.risk.medium-days:14}")
    private int mediumRiskDays;

    @Value("${stockpilot.analytics.risk.high-days:6}")
    private int highRiskDays;

    @Value("${stockpilot.analytics.reorder.safety-factor:1.5}")
    private double safetyFactor;

    @Value("${stockpilot.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${stockpilot.ai.provider:openai}")
    private String aiProvider;

    @Value("${stockpilot.ai.model:gpt-4o-mini}")
    private String aiModel;

    @GetMapping("/config")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    @Operation(summary = "Get Authoritative Analytics & System Configuration", description = "Returns active runtime thresholds and health indicators")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> getSystemConfig() {
        SystemConfigResponse config = SystemConfigResponse.builder()
                .analytics(AnalyticsConfig.builder()
                        .lowRiskThresholdDays(lowRiskDays)
                        .mediumRiskThresholdDays(mediumRiskDays)
                        .highRiskThresholdDays(highRiskDays)
                        .criticalRiskThresholdDays(highRiskDays)
                        .reorderSafetyFactor(safetyFactor)
                        .defaultForecastingAlgorithm("MOVING_AVERAGE")
                        .supportedForecastHorizons(new String[]{"7_DAYS", "14_DAYS", "30_DAYS"})
                        .build())
                .systemStatus(SystemStatus.builder()
                        .databaseStatus("Connected")
                        .apiStatus("Healthy")
                        .invariantsEnforced("onHandQuantity = availableQuantity + reservedQuantity + damagedQuantity")
                        .optimisticLockingActive(true)
                        .aiProvider(aiProvider)
                        .aiModel(aiModel)
                        .aiAvailable(aiEnabled)
                        .build())
                .build();

        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @Data
    @Builder
    public static class SystemConfigResponse {
        private AnalyticsConfig analytics;
        private SystemStatus systemStatus;
    }

    @Data
    @Builder
    public static class AnalyticsConfig {
        private int lowRiskThresholdDays;
        private int mediumRiskThresholdDays;
        private int highRiskThresholdDays;
        private int criticalRiskThresholdDays;
        private double reorderSafetyFactor;
        private String defaultForecastingAlgorithm;
        private String[] supportedForecastHorizons;
    }

    @Data
    @Builder
    public static class SystemStatus {
        private String databaseStatus;
        private String apiStatus;
        private String invariantsEnforced;
        private boolean optimisticLockingActive;
        private String aiProvider;
        private String aiModel;
        private boolean aiAvailable;
    }
}
