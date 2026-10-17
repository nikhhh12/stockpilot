package com.stockpilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpilot.service.ai.LlmClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class LlmClientMockTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("OpenAI format: successfully parses completion message")
    void testOpenAiSuccess() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        LlmClient client = new LlmClient(objectMapper, restClient);
        ReflectionTestUtils.setField(client, "aiEnabled", true);
        ReflectionTestUtils.setField(client, "provider", "openai");
        ReflectionTestUtils.setField(client, "apiKey", "test-openai-key");
        ReflectionTestUtils.setField(client, "model", "gpt-4o-mini");

        String mockOpenAiResponse = """
            {
                "id": "chatcmpl-123",
                "object": "chat.completion",
                "choices": [
                    {
                        "index": 0,
                        "message": {
                            "role": "assistant",
                            "content": "Optimal reorder quantity for SKU-100 is 45 units based on current lead time."
                        },
                        "finish_reason": "stop"
                    }
                ]
            }
            """;

        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-openai-key"))
                .andRespond(withSuccess(mockOpenAiResponse, MediaType.APPLICATION_JSON));

        Optional<String> result = client.generateGroundedCompletion("System instructions", "What is reorder for SKU-100?");

        assertThat(result).isPresent();
        assertThat(result.get()).contains("Optimal reorder quantity for SKU-100 is 45 units");
        server.verify();
    }

    @Test
    @DisplayName("Gemini format: successfully parses candidates content parts text")
    void testGeminiSuccess() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        LlmClient client = new LlmClient(objectMapper, restClient);
        ReflectionTestUtils.setField(client, "aiEnabled", true);
        ReflectionTestUtils.setField(client, "provider", "gemini");
        ReflectionTestUtils.setField(client, "apiKey", "test-gemini-key");
        ReflectionTestUtils.setField(client, "model", "gemini-1.5-flash");

        String mockGeminiResponse = """
            {
                "candidates": [
                    {
                        "content": {
                            "parts": [
                                {
                                    "text": "Stockout risk is HIGH for Warehouse BLR-01 with 4 days supply remaining."
                                }
                            ]
                        }
                    }
                ]
            }
            """;

        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=test-gemini-key"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(mockGeminiResponse, MediaType.APPLICATION_JSON));

        Optional<String> result = client.generateGroundedCompletion("System instructions", "Evaluate risk for BLR-01");

        assertThat(result).isPresent();
        assertThat(result.get()).contains("Stockout risk is HIGH for Warehouse BLR-01");
        server.verify();
    }

    @Test
    @DisplayName("Provider 5xx error or rate limit: returns empty Optional gracefully without throwing")
    void testProviderErrorHandling() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        LlmClient client = new LlmClient(objectMapper, restClient);
        ReflectionTestUtils.setField(client, "aiEnabled", true);
        ReflectionTestUtils.setField(client, "provider", "openai");
        ReflectionTestUtils.setField(client, "apiKey", "test-key");

        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        Optional<String> result = client.generateGroundedCompletion("System", "User query");

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("Malformed JSON response: handled safely, returns empty Optional")
    void testMalformedJsonResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        LlmClient client = new LlmClient(objectMapper, restClient);
        ReflectionTestUtils.setField(client, "aiEnabled", true);
        ReflectionTestUtils.setField(client, "provider", "openai");
        ReflectionTestUtils.setField(client, "apiKey", "test-key");

        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{ unclosed: json ", MediaType.APPLICATION_JSON));

        Optional<String> result = client.generateGroundedCompletion("System", "User query");

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("AI disabled or missing key: returns empty Optional without executing HTTP call")
    void testDisabledAiNoNetworkCall() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        LlmClient client = new LlmClient(objectMapper, restClient);
        ReflectionTestUtils.setField(client, "aiEnabled", false);
        ReflectionTestUtils.setField(client, "apiKey", "mock-key-disabled");

        Optional<String> result = client.generateGroundedCompletion("System", "User query");

        assertThat(result).isEmpty();
        server.verify(); // Verifies no HTTP requests were made
    }
}
