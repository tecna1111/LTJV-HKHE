package com.cosre.cosre_backend.modules.chat.repository;

import com.cosre.cosre_backend.modules.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRoomTypeAndRoomIdOrderByCreatedAtAsc(String roomType, Long roomId);
}
