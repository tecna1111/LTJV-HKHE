package com.cosre.cosre_backend.modules.ai.client;

import com.cosre.cosre_backend.common.exception.ExternalServiceException;
import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class GeminiAIClient implements AIClient {

    private final RestClient restClient;

    @Value("${app.ai.gemini.api-key:}")
    private String apiKey;

    @Value("${app.ai.gemini.model:gemini-2.5-flash}")
    private String model;

    public GeminiAIClient(
            @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${app.ai.timeout-seconds:30}") long timeoutSeconds) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        if (timeoutSeconds < 1 || timeoutSeconds > 120) throw new IllegalArgumentException("AI timeout must be between 1 and 120 seconds");
        int timeoutMs = (int) Duration.ofSeconds(timeoutSeconds).toMillis();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ExternalServiceException(HttpStatus.SERVICE_UNAVAILABLE, "AI chưa được cấu hình API key");
        }

        Map<String, Object> requestBody = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", userPrompt))
                ))
        );

        JsonNode response;
        try {
            String body = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.length() > 1000000) {
                throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, "AI provider trả về dữ liệu rỗng hoặc quá lớn");
            }
            response = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, "AI provider trả về dữ liệu không hợp lệ");
        } catch (ResourceAccessException exception) {
            Throwable cause = exception;
            while (cause != null) {
                if (cause instanceof java.net.SocketTimeoutException) {
                    throw new ExternalServiceException(HttpStatus.GATEWAY_TIMEOUT, "AI provider không phản hồi kịp thời");
                }
                cause = cause.getCause();
            }
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, "Không thể kết nối tới dịch vụ AI");
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 429) {
                throw new ExternalServiceException(HttpStatus.TOO_MANY_REQUESTS, "Đã vượt giới hạn số request tới AI, vui lòng thử lại sau");
            }
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, "AI provider trả về lỗi");
        } catch (org.springframework.web.client.RestClientException exception) {
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, "AI provider trả về dữ liệu không hợp lệ");
        }

        if (response == null) {
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, "AI provider không trả về dữ liệu");
        }

        JsonNode textNode = response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (!textNode.isTextual() || textNode.asText().isBlank() || textNode.asText().length() > 64000) {
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, "AI provider trả về nội dung không hợp lệ");
        }

        return textNode.asText();
    }
}
