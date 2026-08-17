package com.cosre.cosre_backend.modules.chat.service;

import com.cosre.cosre_backend.common.exception.ResourceNotFoundException;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.chat.dto.ChatMessageResponse;
import com.cosre.cosre_backend.modules.chat.dto.SendChatMessageRequest;
import com.cosre.cosre_backend.modules.chat.entity.ChatMessage;
import com.cosre.cosre_backend.modules.chat.entity.ChatRoomType;
import com.cosre.cosre_backend.modules.chat.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class ChatService {
    private final ChatMessageRepository repository;
    private final UserRepository userRepository;
    private final ChatRoomAccessService accessService;

    public ChatService(ChatMessageRepository repository, UserRepository userRepository, ChatRoomAccessService accessService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.accessService = accessService;
    }

    public ChatMessageResponse send(ChatRoomType roomType, Long roomId, String username,
            SendChatMessageRequest request) {
        accessService.requireAccess(username, roomType, roomId);
        User sender = requireUser(username);
        ChatMessage message = new ChatMessage();
        message.setRoomType(roomType.name());
        message.setRoomId(roomId);
        message.setSenderId(sender.getId());
        message.setContent(request.content().trim());
        return ChatMessageResponse.from(repository.save(message), sender);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> history(ChatRoomType roomType, Long roomId, String username) {
        accessService.requireAccess(username, roomType, roomId);
        return repository.findByRoomTypeAndRoomIdOrderByCreatedAtAsc(roomType.name(), roomId).stream()
                .map(message -> ChatMessageResponse.from(message, userRepository.findById(message.getSenderId())
                        .orElseThrow(() -> new ResourceNotFoundException("Message sender not found"))))
                .toList();
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
