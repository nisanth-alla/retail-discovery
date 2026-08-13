package com.innova.visual_retail_discovery.service.chat;

import com.innova.visual_retail_discovery.model.ChatRequest.ChatMessage;

import java.util.List;

public final class FashionChatSupport {

    private FashionChatSupport() {
    }

    public static final String SYSTEM_PROMPT =
            "You are a friendly AI fashion stylist chatbot. Speak naturally, like a real person texting — casual, warm, and concise.\n\n" +
            "Rules:\n" +
            "- Keep replies SHORT (1-2 sentences) unless the user genuinely needs a detailed breakdown.\n" +
            "- If the user's request is vague or missing context (e.g. no occasion, no style preference, no size/body type hints), ask ONE short clarifying question before giving advice. Never assume — a well-targeted suggestion beats a generic one.\n" +
            "- When an image context is provided (products found from visual search), ground your response in those results. Reference the detected items naturally.\n" +
            "- Never make up products or brands you don't know about.\n" +
            "- If a question is unrelated to fashion, gently redirect in one sentence.\n" +
            "- Do NOT use bullet lists or headers for simple questions. Save structured output for complex styling guides only.";

    public static String systemPrompt(String userContext) {
        if (userContext == null || userContext.isBlank()) {
            return SYSTEM_PROMPT;
        }
        return "User profile — " + userContext + ".\nUse this to personalise every recommendation.\n\n" + SYSTEM_PROMPT;
    }

    public static String enrichedUserMessage(String userMessage, List<String> imageContext) {
        if (imageContext == null || imageContext.isEmpty()) {
            return userMessage;
        }
        return "[Visual search found these items from our catalogue: " +
                String.join(", ", imageContext) + "]\n\nUser said: " + userMessage;
    }

    public static boolean isValidRole(String role) {
        return "user".equals(role) || "assistant".equals(role);
    }
}
