package com.stockpilot.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class LlmClient {

    @Value("${stockpilot.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${stockpilot.ai.provider:openai}")
    private String provider;

    @Value("${stockpilot.ai.model:gpt-4o-mini}")
    private String model;

    @Value("${stockpilot.ai.api-key:mock-key-disabled}")
    private String apiKey;

    @Value("${stockpilot.ai.base-url:}")
    private String baseUrl;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public LlmClient(ObjectMapper objectMapper, RestClient restClient) {
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public LlmClient(ObjectMapper objectMapper) {
        this(objectMapper, createDefaultRestClient());
    }

    private static RestClient createDefaultRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        return RestClient.builder().requestFactory(factory).build();
    }

    public boolean isLiveProviderAvailable() {
        return aiEnabled && apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equalsIgnoreCase("mock-key-disabled");
    }

    public Optional<String> generateGroundedCompletion(String systemPrompt, String userMessage) {
        if (!isLiveProviderAvailable()) {
            return Optional.empty();
        }

        try {
            if ("gemini".equalsIgnoreCase(provider)) {
                return callGemini(systemPrompt, userMessage);
            } else {
                return callOpenAi(systemPrompt, userMessage);
            }
        } catch (Exception ex) {
            log.warn("External LLM provider call failed ({}), falling back to deterministic synthesis: {}", provider, ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> callOpenAi(String systemPrompt, String userMessage) {
        String url = (baseUrl != null && !baseUrl.trim().isEmpty()) ? baseUrl : "https://api.openai.com/v1/chat/completions";
        Map<String, Object> payload = Map.of(
                "model", model != null ? model : "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "max_tokens", 800,
                "temperature", 0.2
        );

        String responseStr = restClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);

        if (responseStr == null) {
            return Optional.empty();
        }

        try {
            JsonNode root = objectMapper.readTree(responseStr);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String reply = choices.get(0).path("message").path("content").asText();
                return Optional.ofNullable(reply);
            }
        } catch (Exception e) {
            log.warn("Failed to parse OpenAI completion response: {}", e.getMessage());
        }

        return Optional.empty();
    }

    private Optional<String> callGemini(String systemPrompt, String userMessage) {
        String geminiModel = model != null ? model : "gemini-1.5-flash";
        String url = (baseUrl != null && !baseUrl.trim().isEmpty()) ? baseUrl :
                ("https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent?key=" + apiKey);

        String combinedPrompt = systemPrompt + "\n\nUser Question: " + userMessage;
        Map<String, Object> payload = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", combinedPrompt)))
                )
        );

        String responseStr = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);

        if (responseStr == null) {
            return Optional.empty();
        }

        try {
            JsonNode root = objectMapper.readTree(responseStr);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    String reply = parts.get(0).path("text").asText();
                    return Optional.ofNullable(reply);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse Gemini generation response: {}", e.getMessage());
        }

        return Optional.empty();
    }
}
