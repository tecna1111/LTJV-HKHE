package com.cosre.cosre_backend.modules.collaboration.service;

import com.cosre.cosre_backend.common.exception.DuplicateResourceException;
import com.cosre.cosre_backend.modules.collaboration.dto.CollaborationOperation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Server-sequenced RGA text and per-object compare-and-set canvas operations. */
public final class CollaborationEngine {
    private CollaborationEngine() {}
    public static void apply(String kind, ObjectNode state, CollaborationOperation op, long revision) {
        ObjectNode p = new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(op.payload());
        if (kind.equals("whiteboard")) {
            require(op.action().startsWith("object."), "Thao tác không phù hợp bảng vẽ");
            String id = p.path("id").asText();
            require(id.matches("[a-zA-Z0-9_:-]{1,100}"), "ID đối tượng không hợp lệ");
            JsonNode old = state.get(id);
            long version = old == null ? 0 : old.path("version").asLong();
            require(p.path("expectedVersion").isIntegralNumber(), "Thiếu phiên bản đối tượng");
            if (p.path("expectedVersion").asLong() != version)
                throw new DuplicateResourceException("Đối tượng đã thay đổi. Bản sửa của bạn được giữ lại; hãy xem bản mới trước khi thử lại.");
            ObjectNode item = state.objectNode().put("version", revision);
            if (op.action().equals("object.delete")) {
                require(old != null, "Đối tượng không tồn tại");
                item.put("deleted", true);
            } else {
                JsonNode value = p.get("value");
                require(value != null && value.isObject() && value.path("type").asText().equalsIgnoreCase("path"), "Chỉ chấp nhận nét vẽ");
                require(value.path("path").isArray() && value.path("path").size() <= 20000, "Nét vẽ không hợp lệ hoặc quá dài");
                require(value.toString().length() <= 200000, "Nét vẽ quá lớn");
                // Do not accept image URLs, SVG or arbitrary Fabric object constructors.
                ObjectNode safe = state.objectNode();
                for (String field : new String[]{"type", "path", "left", "top", "width", "height", "scaleX", "scaleY",
                        "angle", "flipX", "flipY", "originX", "originY", "stroke", "strokeWidth", "fill",
                        "strokeLineCap", "strokeLineJoin", "strokeMiterLimit", "opacity", "pathOffset"})
                    if (value.has(field)) safe.set(field, value.get(field));
                require(safe.path("stroke").isTextual() && safe.path("stroke").asText().matches("#[0-9a-fA-F]{6}"), "Màu nét vẽ không hợp lệ");
                safe.putNull("fill");
                item.put("deleted", false).set("value", safe);
            }
            state.set(id, item);
        } else {
            require(op.action().equals("text.edit"), "Thao tác không phù hợp tài liệu");
            String after = p.path("after").asText("");
            require(after.isEmpty() || state.has(after), "Không tìm thấy vị trí chèn; cần đồng bộ lại");
            String text = p.path("text").asText("");
            require(text.length() <= 4000, "Mỗi lần chèn tối đa 4000 ký tự");
            JsonNode deletes = p.path("deleteIds");
            require(deletes.isArray() && deletes.size() <= 4000, "Danh sách xóa không hợp lệ");
            for (JsonNode id : deletes) {
                require(id.isTextual() && state.has(id.asText()), "Ký tự cần xóa không tồn tại");
                ((ObjectNode) state.get(id.asText())).put("deleted", true);
            }
            require(state.size() + text.length() <= 100000, "Tài liệu đạt giới hạn 100000 ký tự kể cả lịch sử xóa");
            // UTF-16 units match browser selection offsets, including surrogate pairs.
            for (int i = 0; i < text.length(); i++) {
                String id = op.operationId() + ":" + i;
                require(!state.has(id), "ID ký tự bị trùng");
                state.set(id, state.objectNode().put("after", after).put("unit", (int) text.charAt(i))
                        .put("order", revision).put("deleted", false));
                after = id;
            }
        }
        require(state.toString().length() <= 8000000, "Nội dung cộng tác vượt giới hạn lưu trữ");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
