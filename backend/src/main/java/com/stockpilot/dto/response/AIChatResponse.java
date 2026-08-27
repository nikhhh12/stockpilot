package com.stockpilot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIChatResponse {
    private String reply;
    private String conversationId;
    private List<String> toolsInvoked;
    private boolean providerAvailable;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
