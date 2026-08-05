
// ─────────────────────────────────────────────
// 7. REST Controller
// ─────────────────────────────────────────────
package com.innova.visual_retail_discovery.controller;

import com.innova.visual_retail_discovery.model.ChatRequest;
import com.innova.visual_retail_discovery.model.ChatResponse;
import com.innova.visual_retail_discovery.service.anthropic.AnthropicFashionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fashion")
public class FashionChatController {

    private final AnthropicFashionService fashionService;

    public FashionChatController(AnthropicFashionService fashionService) {
        this.fashionService = fashionService;
    }

    /**
     * POST /api/fashion/chat
     * Body: { "message": "What should I wear to a summer wedding?" }
     */
    
    @CrossOrigin(origins = "${app.cors.origin}")
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse("Please provide a fashion question!"));
        }
        try {
            String reply = fashionService.chat(request.getMessage(), request.getConversationHistory(), request.getImageContext(), request.getUserContext());
            return ResponseEntity.ok(new ChatResponse(reply));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ChatResponse("AI stylist is temporarily unavailable."));
        }
    }
}