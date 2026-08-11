package com.cosre.cosre_backend.modules.ai.service;


import com.cosre.cosre_backend.modules.ai.dto.ChatRequest;
import com.cosre.cosre_backend.modules.ai.dto.ChatResponse;
import com.cosre.cosre_backend.modules.ai.entity.ChatHistory;
import com.cosre.cosre_backend.modules.ai.repository.ChatHistoryRepository;
import org.springframework.stereotype.Service;
@Service
public class AIService {
    private final ChatHistoryRepository chatHistoryRepository;

    public AIService(ChatHistoryRepository chatHistoryRepository) {
        this.chatHistoryRepository = chatHistoryRepository;
    }

    public ChatResponse chat(ChatRequest request) {

        String answer = "AI chưa được tích hợp. Bạn vừa nhập: " + request.prompt();

        ChatHistory history = new ChatHistory();
        history.setPrompt(request.prompt());
        history.setAnswer(answer);

        chatHistoryRepository.save(history);

        return new ChatResponse(answer);
    }

}
