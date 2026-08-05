package com.innova.visual_retail_discovery.model;

import java.util.List;

public class ChatRequest {

    private String message;

    /** Each entry: {"role": "user"|"assistant", "content": "..."} */
    private List<ChatMessage> conversationHistory;

    /** Product names/labels returned by visual search when an image was uploaded */
    private List<String> imageContext;

    /** User profile context collected via onboarding (occasion, age, gender) */
    private String userContext;

    public ChatRequest() {}

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<ChatMessage> getConversationHistory() { return conversationHistory; }
    public void setConversationHistory(List<ChatMessage> conversationHistory) { this.conversationHistory = conversationHistory; }

    public List<String> getImageContext() { return imageContext; }
    public void setImageContext(List<String> imageContext) { this.imageContext = imageContext; }

    public String getUserContext() { return userContext; }
    public void setUserContext(String userContext) { this.userContext = userContext; }

    public static class ChatMessage {
        private String role;
        private String content;

        public ChatMessage() {}
        public ChatMessage(String role, String content) { this.role = role; this.content = content; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}

