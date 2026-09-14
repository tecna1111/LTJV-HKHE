package com.cosre.cosre_backend.modules.ai.controller;
import com.cosre.cosre_backend.common.dto.ApiResponse;
import com.cosre.cosre_backend.modules.ai.dto.ChatRequest;
import com.cosre.cosre_backend.modules.ai.dto.ChatResponse;
import com.cosre.cosre_backend.modules.ai.dto.GenerateMilestonesRequest;
import com.cosre.cosre_backend.modules.ai.dto.GenerateMilestonesResponse;
import com.cosre.cosre_backend.modules.ai.service.AIService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/ai")
public class AIController {
    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('LECTURER','STUDENT')")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @Valid @RequestBody ChatRequest request, Authentication auth) {

        ChatResponse response = aiService.chat(request, auth.getName());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "AI response generated successfully",
                        response
                )
        );
    }

    @PostMapping("/milestones/generate")
    @PreAuthorize("hasAnyRole('LECTURER','STUDENT')")
    public ResponseEntity<ApiResponse<GenerateMilestonesResponse>> generateMilestones(
            @Valid @RequestBody GenerateMilestonesRequest request, Authentication auth) {

        GenerateMilestonesResponse response = aiService.generateMilestones(request, auth.getName());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Milestones generated",
                        response
                )
        );
    }

    @GetMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
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