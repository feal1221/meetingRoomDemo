package com.meet.meetingRoomDemo.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final AnthropicClient client;
    private final ToolHandler toolHandler;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${anthropic.model}")
    private String model;

    private static final Duration HISTORY_TTL   = Duration.ofMinutes(30);
    private static final int      MAX_ITERATIONS = 5;
    private static final String   SYSTEM_PROMPT  = """
        你是一個智能的會議室/教室預約助理，可以幫助用戶：
        1. 查詢所有可用的會議室
        2. 查詢特定日期的可用時段
        3. 建立會議室預約
        4. 查看自己的預約記錄
        5. 取消預約

        回答請使用繁體中文，語氣友善專業。
        建立預約前必須確認：房間、日期與時間（需明確的開始和結束時間）、預約主題。
        時間請使用 ISO-8601 格式含台北時區，例如：2024-01-15T09:00:00+08:00
        """;

    public String chat(String userMessage, UUID userId) {
        String historyKey = "chat:" + userId;
        List<ConversationTurn> history = loadHistory(historyKey);

        MessageCreateParams.Builder builder = MessageCreateParams.builder()
            .model(model)
            .maxTokens(2048L)
            .system(SYSTEM_PROMPT)
            .addTool(toolListRooms())
            .addTool(toolCheckAvailability())
            .addTool(toolCreateBooking())
            .addTool(toolListMyBookings())
            .addTool(toolCancelBooking());

        for (ConversationTurn turn : history) {
            if ("user".equals(turn.getRole())) {
                builder.addUserMessage(turn.getContent());
            } else {
                builder.addAssistantMessage(turn.getContent());
            }
        }
        builder.addUserMessage(userMessage);

        String finalReply = null;
        int iterations = 0;

        while (finalReply == null && iterations++ < MAX_ITERATIONS) {
            Message response = client.messages().create(builder.build());

            List<ContentBlock> toolUseBlocks = response.content().stream()
                .filter(ContentBlock::isToolUse)
                .collect(Collectors.toList());

            if (toolUseBlocks.isEmpty()) {
                finalReply = response.content().stream()
                    .filter(ContentBlock::isText)
                    .map(b -> b.text().map(TextBlock::text).orElse(""))
                    .collect(Collectors.joining("\n"));
            } else {
                // 加入 Claude 的 tool_use 回應（addMessage 接受 Message 物件）
                builder.addMessage(response);

                // 執行所有工具並收集結果
                List<ContentBlockParam> toolResults = new ArrayList<>();
                for (ContentBlock block : toolUseBlocks) {
                    block.toolUse().ifPresent(toolUse -> {
                        try {
                            JsonNode inputNode = toolUse._input().convert(JsonNode.class);
                            String toolResult = toolHandler.execute(toolUse.name(), inputNode, userId);
                            log.info("Tool '{}' → {}",
                                toolUse.name(),
                                toolResult.length() > 120 ? toolResult.substring(0, 120) + "…" : toolResult);
                            toolResults.add(ContentBlockParam.ofToolResult(
                                ToolResultBlockParam.builder()
                                    .toolUseId(toolUse.id())
                                    .content(toolResult)
                                    .build()
                            ));
                        } catch (Exception e) {
                            log.error("Tool '{}' failed: {}", toolUse.name(), e.getMessage());
                            toolResults.add(ContentBlockParam.ofToolResult(
                                ToolResultBlockParam.builder()
                                    .toolUseId(toolUse.id())
                                    .content("Error: " + e.getMessage())
                                    .build()
                            ));
                        }
                    });
                }
                builder.addUserMessageOfBlockParams(toolResults);
            }
        }

        if (finalReply == null || finalReply.isBlank()) {
            finalReply = "抱歉，處理您的請求時發生問題，請再試一次。";
        }

        history.add(new ConversationTurn("user", userMessage));
        history.add(new ConversationTurn("assistant", finalReply));
        saveHistory(historyKey, history);

        return finalReply;
    }

    public void clearHistory(UUID userId) {
        redisTemplate.delete("chat:" + userId);
    }

    // ─── Redis 對話歷史 ────────────────────────────────────────────────────────

    private List<ConversationTurn> loadHistory(String key) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize chat history for key {}: {}", key, e.getMessage());
            return new ArrayList<>();
        }
    }

    private void saveHistory(String key, List<ConversationTurn> history) {
        List<ConversationTurn> trimmed = history.size() > 20
            ? history.subList(history.size() - 20, history.size())
            : history;
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(trimmed), HISTORY_TTL);
        } catch (Exception e) {
            log.warn("Failed to save chat history: {}", e.getMessage());
        }
    }

    // ─── Tool 定義 ────────────────────────────────────────────────────────────

    private Tool toolListRooms() {
        return Tool.builder()
            .name("list_rooms")
            .description("查詢所有目前啟用中的會議室清單，包含房間名稱、容量、位置")
            .inputSchema(Tool.InputSchema.builder()
                .type(JsonValue.from("object"))
                .properties(Tool.InputSchema.Properties.builder().build())
                .build())
            .build();
    }

    private Tool toolCheckAvailability() {
        return Tool.builder()
            .name("check_availability")
            .description("查詢指定日期所有會議室的預約情況，了解哪些時段已被佔用")
            .inputSchema(Tool.InputSchema.builder()
                .type(JsonValue.from("object"))
                .properties(Tool.InputSchema.Properties.builder()
                    .putAdditionalProperty("date", JsonValue.from(Map.of(
                        "type", "string",
                        "description", "查詢日期，格式 yyyy-MM-dd，例如 2024-01-15"
                    )))
                    .build())
                .required(List.of("date"))
                .build())
            .build();
    }

    private Tool toolCreateBooking() {
        return Tool.builder()
            .name("create_booking")
            .description("為當前登入的用戶建立一筆會議室預約")
            .inputSchema(Tool.InputSchema.builder()
                .type(JsonValue.from("object"))
                .properties(Tool.InputSchema.Properties.builder()
                    .putAdditionalProperty("room_id", JsonValue.from(Map.of(
                        "type", "string",
                        "description", "會議室 ID（UUID 格式）"
                    )))
                    .putAdditionalProperty("title", JsonValue.from(Map.of(
                        "type", "string",
                        "description", "預約主題或名稱"
                    )))
                    .putAdditionalProperty("started_time", JsonValue.from(Map.of(
                        "type", "string",
                        "description", "開始時間，ISO-8601 含時區，例如 2024-01-15T09:00:00+08:00"
                    )))
                    .putAdditionalProperty("ended_time", JsonValue.from(Map.of(
                        "type", "string",
                        "description", "結束時間，ISO-8601 含時區，例如 2024-01-15T10:00:00+08:00"
                    )))
                    .putAdditionalProperty("reminder_minutes", JsonValue.from(Map.of(
                        "type", "integer",
                        "description", "提前幾分鐘發送提醒 Email（可選，例如 15）"
                    )))
                    .build())
                .required(List.of("room_id", "title", "started_time", "ended_time"))
                .build())
            .build();
    }

    private Tool toolListMyBookings() {
        return Tool.builder()
            .name("list_my_bookings")
            .description("查詢當前登入用戶的預約記錄")
            .inputSchema(Tool.InputSchema.builder()
                .type(JsonValue.from("object"))
                .properties(Tool.InputSchema.Properties.builder()
                    .putAdditionalProperty("upcoming_only", JsonValue.from(Map.of(
                        "type", "boolean",
                        "description", "是否只顯示未來的預約，預設 true"
                    )))
                    .build())
                .build())
            .build();
    }

    private Tool toolCancelBooking() {
        return Tool.builder()
            .name("cancel_booking")
            .description("取消當前登入用戶的一筆預約（只能取消自己的預約）")
            .inputSchema(Tool.InputSchema.builder()
                .type(JsonValue.from("object"))
                .properties(Tool.InputSchema.Properties.builder()
                    .putAdditionalProperty("record_id", JsonValue.from(Map.of(
                        "type", "string",
                        "description", "要取消的預約 ID（UUID 格式）"
                    )))
                    .build())
                .required(List.of("record_id"))
                .build())
            .build();
    }
}
