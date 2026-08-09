package com.cosre.cosre_backend.modules.chat.repository;

import com.cosre.cosre_backend.modules.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // Hàm hỗ trợ lấy lịch sử chat theo phòng
    List<ChatMessage> findByRoomTypeAndRoomIdOrderByCreatedAtAsc(String roomType, Long roomId);
}