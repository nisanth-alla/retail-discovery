package com.innova.visual_retail_discovery.service.chat;

import com.innova.visual_retail_discovery.model.ChatRequest.ChatMessage;

import java.util.List;

public interface FashionChatService {
    String chat(String userMessage, List<ChatMessage> history, List<String> imageContext, String userContext);
}
