package com.cosre.cosre_backend.modules.chat.service;

public interface ChatRoomAccessService {
    boolean canAccess(Long userId, String roomType, Long roomId);
}
