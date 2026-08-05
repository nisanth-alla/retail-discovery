package com.innova.visual_retail_discovery.service.anthropic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.innova.visual_retail_discovery.model.ChatRequest.ChatMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class AnthropicFashionService {

    private static final String FASHION_SYSTEM_PROMPT =
            "You are a friendly AI fashion stylist chatbot. Speak naturally, like a real person texting — casual, warm, and concise.\n\n" +
            "Rules:\n" +
            "- Keep replies SHORT (1-2 sentences) unless the user genuinely needs a detailed breakdown.\n" +
            "- If the user's request is vague or missing context (e.g. no occasion, no style preference, no size/body type hints), ask ONE short clarifying question before giving advice. Never assume — a well-targeted suggestion beats a generic one.\n" +
            "- When an image context is provided (products found from visual search), ground your response in those results. Reference the detected items naturally.\n" +
            "- Never make up products or brands you don't know about.\n" +
            "- If a question is unrelated to fashion, gently redirect in one sentence.\n" +
            "- Do NOT use bullet lists or headers for simple questions. Save structured output for complex styling guides only.";

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.api.url}")
    private String apiUrl;

    @Value("${anthropic.model}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AnthropicFashionService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String chat(String userMessage, List<ChatMessage> history, List<String> imageContext, String userContext) {
        // Build request body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 1024);

        // Prepend user profile to system prompt when available
        String systemPrompt = FASHION_SYSTEM_PROMPT;
        if (userContext != null && !userContext.isBlank()) {
            systemPrompt = "User profile — " + userContext + ".\nUse this to personalise every recommendation.\n\n" + FASHION_SYSTEM_PROMPT;
        }
        body.put("system", systemPrompt);

        ArrayNode messages = body.putArray("messages");

        // Inject conversation history for multi-turn context
        if (history != null) {
            for (ChatMessage msg : history) {
                ObjectNode m = messages.addObject();
                m.put("role", msg.getRole());
                m.put("content", msg.getContent());
            }
        }

        // Enrich the user message with image search context if available
        String enrichedMessage = userMessage;
        if (imageContext != null && !imageContext.isEmpty()) {
            enrichedMessage = "[Visual search found these items from our catalogue: " +
                    String.join(", ", imageContext) + "]\n\nUser said: " + userMessage;
        }

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

