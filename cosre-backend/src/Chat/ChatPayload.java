package com.cosre.cosre_backend.modules.chat.dto;

import lombok.Data;

@Data
public class ChatPayload {
    private String roomType;
    private Long roomId;
    private String content;
    
    // Server sẽ tự động điền các trường này trước khi gửi xuống lại Client
    private Long senderId; 
    private String senderName; 
    private String timestamp;
}