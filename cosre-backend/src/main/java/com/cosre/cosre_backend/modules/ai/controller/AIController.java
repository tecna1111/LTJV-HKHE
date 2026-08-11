package com.cosre.cosre_backend.modules.ai.controller;


import com.cosre.cosre_backend.common.dto.ApiResponse;
import com.cosre.cosre_backend.modules.ai.dto.ChatRequest;
import com.cosre.cosre_backend.modules.ai.dto.ChatResponse;
import com.cosre.cosre_backend.modules.ai.service.AIService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/ai")
public class AIController {
    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @Valid @RequestBody ChatRequest request) {

        ChatResponse response = aiService.chat(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "AI response generated successfully",
                        response
                )
        );
    }

    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> test() {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "AI module is running",
                        "OK"
                )
        );
    }

}
