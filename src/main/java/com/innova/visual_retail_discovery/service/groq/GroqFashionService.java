package com.innova.visual_retail_discovery.service.groq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.innova.visual_retail_discovery.model.ChatRequest.ChatMessage;
import com.innova.visual_retail_discovery.service.chat.FashionChatService;
import com.innova.visual_retail_discovery.service.chat.FashionChatSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@ConditionalOnProperty(name = "chat.provider", havingValue = "groq", matchIfMissing = true)
public class GroqFashionService implements FashionChatService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String apiUrl;
    private final String model;

    public GroqFashionService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${groq.api.key:}") String apiKey,
            @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}") String apiUrl,
            @Value("${groq.model:llama-3.3-70b-versatile}") String model) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
    }

    @Override
    public String chat(String userMessage, List<ChatMessage> history, List<String> imageContext, String userContext) {
        requireApiKey();

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0.7);
        body.put("max_tokens", 512);

        ArrayNode messages = body.putArray("messages");
        ObjectNode systemMessage = messages.addObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", FashionChatSupport.systemPrompt(userContext));

        if (history != null) {
            for (ChatMessage message : history) {
                if (message != null && message.getContent() != null &&
                        FashionChatSupport.isValidRole(message.getRole())) {
                    ObjectNode historyMessage = messages.addObject();
                    historyMessage.put("role", message.getRole());
                    historyMessage.put("content", message.getContent());
                }
            }
        }

        ObjectNode userMessageNode = messages.addObject();
        userMessageNode.put("role", "user");
        userMessageNode.put("content", FashionChatSupport.enrichedUserMessage(userMessage, imageContext));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            String requestBody = objectMapper.writeValueAsString(body);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    String.class);
            return parseResponse(response.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Groq chat request failed", e);
        }
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GROQ_API_KEY is not configured");
        }
    }

    private String parseResponse(String responseBody) {
        try {
            JsonNode content = objectMapper.readTree(responseBody).path("choices").path(0).path("message").path("content");
            if (content.isTextual() && !content.asText().isBlank()) {
                return content.asText();
            }
            throw new IllegalStateException("Groq response did not contain message content");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Groq response", e);
        }
    }
}
