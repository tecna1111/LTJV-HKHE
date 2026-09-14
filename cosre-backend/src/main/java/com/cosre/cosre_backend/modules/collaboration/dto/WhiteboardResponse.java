package com.cosre.cosre_backend.modules.collaboration.dto;

import com.cosre.cosre_backend.modules.collaboration.entity.Whiteboard;
import java.time.LocalDateTime;
public record WhiteboardResponse(
        Long id,
        Long teamId,
        String canvasData,
        Long version,
        Long updatedBy,
        LocalDateTime updatedAt
) {
    private static final String EMPTY_CANVAS = "{\"version\":\"6.0.0\",\"objects\":[]}";

    public static WhiteboardResponse from(Whiteboard whiteboard) {
        return new WhiteboardResponse(
                whiteboard.getId(), whiteboard.getTeamId(), whiteboard.getCanvasData(),
                whiteboard.getVersion(), whiteboard.getUpdatedBy(), whiteboard.getUpdatedAt()
        );
    }

    public static WhiteboardResponse empty(Long teamId) {
        return new WhiteboardResponse(null, teamId, EMPTY_CANVAS, 0L, null, null);
    }
}