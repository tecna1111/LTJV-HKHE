package com.cosre.cosre_backend.modules.chat.service;

import com.cosre.cosre_backend.modules.chat.entity.ChatRoomType;

public interface ChatRoomAccessService {
    boolean canAccess(String username, ChatRoomType roomType, Long roomId);
    void requireAccess(String username, ChatRoomType roomType, Long roomId);
}
