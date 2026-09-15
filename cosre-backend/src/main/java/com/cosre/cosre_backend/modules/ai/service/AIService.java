package com.cosre.cosre_backend.modules.ai.service;
import com.cosre.cosre_backend.common.exception.BusinessRuleException;
import com.cosre.cosre_backend.common.exception.ExternalServiceException;
import com.cosre.cosre_backend.common.exception.ResourceNotFoundException;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.ai.client.AIClient;
import com.cosre.cosre_backend.modules.ai.dto.ChatRequest;
import com.cosre.cosre_backend.modules.ai.dto.ChatResponse;
import com.cosre.cosre_backend.modules.ai.dto.GenerateMilestonesRequest;
import com.cosre.cosre_backend.modules.ai.dto.GenerateMilestonesResponse;
import com.cosre.cosre_backend.modules.ai.dto.GenerateMilestonesResponse.GeneratedMilestone;
import com.cosre.cosre_backend.modules.ai.entity.ChatHistory;
import com.cosre.cosre_backend.modules.ai.repository.ChatHistoryRepository;
import com.cosre.cosre_backend.modules.syllabus.entity.Syllabus;
import com.cosre.cosre_backend.modules.syllabus.repository.SyllabusRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class AIService {

    private static final String CHAT_SYSTEM_PROMPT =
            "Bạn là trợ lý học tập của hệ thống COSRE. Trả lời ngắn gọn, rõ ràng, bằng tiếng Việt trừ khi được hỏi bằng ngôn ngữ khác.";

    private static final String MILESTONE_SYSTEM_PROMPT = """
            Bạn là trợ lý lập kế hoạch đồ án học thuật.
            Hãy tạo từ 3 đến 8 milestones dựa trên đề cương và mục tiêu.
            Mỗi milestone phải có title, description và dueOffsetDays.
            dueOffsetDays phải tăng dần và là số nguyên dương.
            Chỉ trả về JSON hợp lệ theo đúng schema, không thêm Markdown, không thêm giải thích:
            {"milestones":[{"title":"...","description":"...","dueOffsetDays":7}]}
            """;

    private static final int MIN_MILESTONES = 3;
    private static final int MAX_MILESTONES = 8;

    private final AIClient aiClient;
    private final ChatHistoryRepository chatHistoryRepository;
    private final UserRepository userRepository;
    private final SyllabusRepository syllabusRepository;
    private final AIContextService contextService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    public AIService(AIClient aiClient, ChatHistoryRepository chatHistoryRepository, UserRepository userRepository,
            SyllabusRepository syllabusRepository, AIContextService contextService) {
        this.aiClient = aiClient;
        this.chatHistoryRepository = chatHistoryRepository;
        this.userRepository = userRepository;
        this.syllabusRepository = syllabusRepository;
        this.contextService = contextService;
    }

    @Transactional(readOnly = true)
    public List<com.cosre.cosre_backend.modules.ai.dto.ChatHistoryResponse> history(Long teamId, String username) {
        User user = requireUser(username);
        if (teamId != null) teamAccessService.requireViewAccess(teamId, username);
        var entries = new ArrayList<>(chatHistoryRepository.findTop100ByUserIdAndTeamIdOrderByIdDesc(user.getId(), teamId));
        java.util.Collections.reverse(entries);
        return entries.stream().map(item -> new com.cosre.cosre_backend.modules.ai.dto.ChatHistoryResponse(
                item.getId(), item.getPrompt(), item.getAnswer(), item.getCreatedAt())).toList();
    }

    public ChatResponse chat(ChatRequest request, String username) {
        User user = requireUser(username);
        String context = request.teamId() == null ? "" : contextService.teamContext(request.teamId(), username);
        String answer = aiClient.generate(CHAT_SYSTEM_PROMPT,
                context + "\nCâu hỏi người dùng:\n" + request.prompt());
        if (answer == null || answer.isBlank() || answer.length() > 16000) {
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, "AI trả về câu trả lời rỗng hoặc quá dài");
        }

        ChatHistory history = new ChatHistory();
        history.setPrompt(request.prompt());
        history.setAnswer(answer);
        history.setUserId(user.getId());
        history.setTeamId(request.teamId());
        chatHistoryRepository.save(history);

        return new ChatResponse(answer);
    }

    @Transactional(readOnly = true)
    public GenerateMilestonesResponse generateMilestones(GenerateMilestonesRequest request, String username) {
        User actor = requireUser(username);

        Syllabus syllabus = syllabusRepository.findDetailedById(request.syllabusId())
                .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found"));
        contextService.requireSyllabusAccess(syllabus, actor);
        if (!syllabus.isActive()) {
            throw new BusinessRuleException("Syllabus is not active");
        }

        String userPrompt = buildMilestonePrompt(syllabus);
        if (request.objectives() != null && !request.objectives().isEmpty()) {
            userPrompt += "\nMục tiêu dự án do giảng viên cung cấp:\n" + String.join("\n", request.objectives());
        }
        String raw = aiClient.generate(MILESTONE_SYSTEM_PROMPT, userPrompt);

        return parseAndValidate(raw);
    }

    private String buildMilestonePrompt(Syllabus syllabus) {
        StringBuilder builder = new StringBuilder();
        builder.append("Môn học: ").append(syllabus.getSubject().getName()).append('\n');
        builder.append("Đề cương: ").append(syllabus.getTitle()).append('\n');
        if (syllabus.getObjectives() != null && !syllabus.getObjectives().isBlank()) {
            builder.append("Mục tiêu:\n").append(AIContextService.bounded(syllabus.getObjectives(), 8000)).append('\n');
        }
        if (syllabus.getContent() != null && !syllabus.getContent().isBlank()) {
            builder.append("Nội dung:\n").append(AIContextService.bounded(syllabus.getContent(), 16000)).append('\n');
        }
        return builder.toString();
    }

    private GenerateMilestonesResponse parseAndValidate(String raw) {
        if (raw == null || raw.length() > 64000)
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, "AI trả về dữ liệu rỗng hoặc quá dài");
        JsonNode root;
        try {
            root = objectMapper.readTree(stripMarkdownFences(raw));
        } catch (Exception exception) {
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, "AI trả về dữ liệu không đúng định dạng JSON");
        }

        if (root == null || !root.isObject()) {
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, "AI trả về dữ liệu không đúng định dạng JSON");
        }
        JsonNode milestonesNode = root.path("milestones");
        if (!milestonesNode.isArray()) {
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, "AI trả về dữ liệu không đúng định dạng JSON");
        }

        List<GeneratedMilestone> milestones = new ArrayList<>();
        for (JsonNode node : milestonesNode) {
            if (!node.path("title").isTextual() || !node.path("description").isTextual()
                    || node.path("title").asText().length() > 200
                    || node.path("description").asText().isBlank()
                    || node.path("description").asText().length() > 1000) {
                throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, "AI trả về title hoặc description không hợp lệ");
            }
            String title = node.path("title").asText(null);
            String description = node.path("description").asText(null);
            if (!node.path("dueOffsetDays").isIntegralNumber() || !node.path("dueOffsetDays").canConvertToInt()) {
                throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, "AI trả về milestone thiếu dueOffsetDays hợp lệ");
            }
            Integer dueOffsetDays = node.path("dueOffsetDays").asInt();
            milestones.add(new GeneratedMilestone(title, description, dueOffsetDays));
        }

        validateMilestones(milestones);
        return new GenerateMilestonesResponse(milestones);
    }

    private void validateMilestones(List<GeneratedMilestone> milestones) {
        if (milestones.size() < MIN_MILESTONES || milestones.size() > MAX_MILESTONES) {
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY,
                    "AI phải trả về từ " + MIN_MILESTONES + " đến " + MAX_MILESTONES + " milestones");
        }

        Set<String> seenTitles = new HashSet<>();
        int previousOffset = 0;
        for (GeneratedMilestone milestone : milestones) {
            if (milestone.title() == null || milestone.title().isBlank()) {
                throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, "Milestone title không được để trống");
            }
            if (milestone.dueOffsetDays() == null || milestone.dueOffsetDays() <= 0) {
                throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, "dueOffsetDays phải là số nguyên dương");
            }
            if (milestone.dueOffsetDays() <= previousOffset) {
                throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, "dueOffsetDays của các milestone phải tăng dần");
            }
            previousOffset = milestone.dueOffsetDays();

            String normalizedTitle = milestone.title().trim().toLowerCase(java.util.Locale.ROOT);
            if (!seenTitles.add(normalizedTitle)) {
                throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, "Các milestone không được trùng title");
            }
        }
    }

    private String stripMarkdownFences(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(json)?", "").trim();
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
