package com.stockpilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AIChatRequest {

    @NotBlank(message = "Prompt query is required")
    private String message;

    private String conversationId;
}
