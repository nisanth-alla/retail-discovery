package com.innova.visual_retail_discovery.service.anthropic;

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
@ConditionalOnProperty(name = "chat.provider", havingValue = "anthropic")
public class AnthropicFashionService implements FashionChatService {

    @Value("${anthropic.api.key:}")
    private String apiKey;

    @Value("${anthropic.api.url:https://api.anthropic.com/v1/messages}")
    private String apiUrl;

    @Value("${anthropic.model:claude-sonnet-4-20250514}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AnthropicFashionService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String chat(String userMessage, List<ChatMessage> history, List<String> imageContext, String userContext) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY is not configured");
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 1024);

        // Prepend user profile to system prompt when available
        body.put("system", FashionChatSupport.systemPrompt(userContext));

        ArrayNode messages = body.putArray("messages");

        // Inject conversation history for multi-turn context
        if (history != null) {
            for (ChatMessage msg : history) {
                if (msg != null && msg.getContent() != null && FashionChatSupport.isValidRole(msg.getRole())) {
                    ObjectNode m = messages.addObject();
                    m.put("role", msg.getRole());
                    m.put("content", msg.getContent());
                }
            }
        }

        // Enrich the user message with image search context if available
        String enrichedMessage = FashionChatSupport.enrichedUserMessage(userMessage, imageContext);

        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", enrichedMessage);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<String> entity;
        try {
            entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize request body: " + e.getMessage(), e);
        }

        String responseStr = "";

        try{
            // Call Anthropic API
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            responseStr = response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Anthropic API unavailable: " + e.getMessage(), e);
        }



        // Parse and return the text content
        try {
            JsonNode root = objectMapper.readTree(responseStr);
            return root.path("content")
                    .get(0)
                    .path("text")
                    .asText("Sorry, I couldn't generate a response.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Anthropic response: " + e.getMessage(), e);
        }
    }
}
