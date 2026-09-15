package com.cosre.cosre_backend.modules.collaboration.service;

import com.cosre.cosre_backend.common.exception.DuplicateResourceException;
import com.cosre.cosre_backend.modules.collaboration.dto.CollaborationOperation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.Map;

@Service
public class CollaborationService {
    private final JdbcTemplate db;
    private final CollaborationAccessService access;
    private final SimpMessagingTemplate messaging;
    private final ObjectMapper json = new ObjectMapper().configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    // Plain maps keep the API compatible with Spring's Jackson 3 and the existing Jackson 2 clients.
    public record Snapshot(long teamId, String kind, long revision, Map<String, Object> state) {}
    private Map<String, Object> asMap(ObjectNode state) {
        return json.convertValue(state, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
    }
    public CollaborationService(JdbcTemplate db, CollaborationAccessService access, SimpMessagingTemplate messaging) {
        this.db = db; this.access = access; this.messaging = messaging;
    }
    private ObjectNode parse(String value) {
        try { return (ObjectNode) json.readTree(value); }
        catch (Exception e) { throw new IllegalStateException("Nội dung lưu trữ không hợp lệ", e); }
    }
    public static void requireKind(String kind) {
        if (!kind.equals("whiteboard") && !kind.equals("text")) throw new IllegalArgumentException("Loại tài liệu không hợp lệ");
    }
    private void lockAndInitialize(long teamId, String kind, String username) {
        requireKind(kind);
        access.requireAccess(teamId, username);
        // The existing team row serializes first creation as well as updates across server instances.
        db.queryForObject("SELECT id FROM teams WHERE id = ? FOR UPDATE", Long.class, teamId);
        if (db.queryForObject("SELECT COUNT(*) FROM collaboration_documents WHERE team_id=? AND kind=?", Integer.class, teamId, kind) == 0) {
            ObjectNode state = json.createObjectNode();
            if (kind.equals("whiteboard")) {
                db.query("SELECT canvas_data FROM whiteboards WHERE team_id=?", rs -> {
                    var canvas = parse(rs.getString(1));
                    int i = 0;
                    for (var object : canvas.path("objects")) {
                        if (!object.path("type").asText().equalsIgnoreCase("path")) continue;
                        ObjectNode item = json.createObjectNode().put("version", 0).put("deleted", false);
                        item.set("value", object);
                        state.set("legacy-" + i++, item);
                    }
                }, teamId);
            }
            db.update("INSERT INTO collaboration_documents(team_id,kind,revision,content) VALUES(?,?,0,?)", teamId, kind, state.toString());
        }
    }
    private Snapshot read(long teamId, String kind) {
        return db.queryForObject("SELECT revision,content FROM collaboration_documents WHERE team_id=? AND kind=?",
                (rs, row) -> new Snapshot(teamId, kind, rs.getLong(1), asMap(parse(rs.getString(2)))), teamId, kind);
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Snapshot get(long teamId, String kind, String username) {
        lockAndInitialize(teamId, kind, username);
        return read(teamId, kind);
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Snapshot apply(long teamId, String kind, String username, CollaborationOperation op) {
        lockAndInitialize(teamId, kind, username);
        if (op == null || op.operationId() == null || !op.operationId().matches("[a-zA-Z0-9_-]{8,80}")
                || op.payload() == null || op.action() == null || !op.action().matches("object.put|object.delete|text.edit"))
            throw new IllegalArgumentException("Thao tác không hợp lệ");
        String payload = op.action() + ":" + json.valueToTree(op.payload()).toString();
        if (payload.length() > 250000) throw new IllegalArgumentException("Thao tác quá lớn");
        var previous = db.queryForList("SELECT actor,payload FROM collaboration_operations WHERE team_id=? AND kind=? AND operation_id=?", teamId, kind, op.operationId());
        if (!previous.isEmpty()) {
            if (!username.equals(previous.get(0).get("actor")) || !payload.equals(previous.get(0).get("payload")))
                throw new DuplicateResourceException("ID thao tác đã được sử dụng với nội dung khác");
            return read(teamId, kind);
        }
        Snapshot old = read(teamId, kind);
        long revision = old.revision() + 1;
        ObjectNode state = json.valueToTree(old.state());
        CollaborationEngine.apply(kind, state, op, revision);
        db.update("UPDATE collaboration_documents SET content=?,revision=? WHERE team_id=? AND kind=?", state.toString(), revision, teamId, kind);
        db.update("INSERT INTO collaboration_operations(team_id,kind,operation_id,actor,payload,revision) VALUES(?,?,?,?,?,?)",
                teamId, kind, op.operationId(), username, payload, revision);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                // No content on long-lived subscriptions: subsequent GET rechecks current membership.
                messaging.convertAndSend("/topic/collaboration/teams/" + teamId + "/" + kind, (Object) Map.of("changed", true));
            }
        });
        return new Snapshot(teamId, kind, revision, asMap(state));
    }
}
