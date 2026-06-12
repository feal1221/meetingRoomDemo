package com.meet.meetingRoomDemo.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meet.meetingRoomDemo.domain.record.RecordDTO;
import com.meet.meetingRoomDemo.domain.record.RecordService;
import com.meet.meetingRoomDemo.domain.record.RecordVO;
import com.meet.meetingRoomDemo.domain.room.RoomService;
import com.meet.meetingRoomDemo.domain.room.RoomVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ToolHandler {

    private final RoomService roomService;
    private final RecordService recordService;
    private final ObjectMapper objectMapper;

    /**
     * 執行 Claude 指定的工具，input 為從 SDK JsonValue 轉換的 JsonNode。
     */
    public String execute(String toolName, JsonNode input, UUID userId) {
        try {

            return switch (toolName) {
                case "list_rooms"          -> handleListRooms();
                case "check_availability" -> handleCheckAvailability(input);
                case "create_booking"     -> handleCreateBooking(input, userId);
                case "list_my_bookings"   -> handleListMyBookings(input, userId);
                case "cancel_booking"     -> handleCancelBooking(input, userId);
                default -> "Unknown tool: " + toolName;
            };
        } catch (Exception e) {
            log.warn("Tool '{}' execution failed: {}", toolName, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    // ─── Tool 實作 ────────────────────────────────────────────────────────────

    private String handleListRooms() throws Exception {
        List<RoomVO> rooms = roomService.getAllActiveRooms();
        List<Map<String, Object>> result = rooms.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("roomId",   r.getRoomId());
            m.put("roomName", r.getRoomName());
            m.put("capacity", r.getCapacity());
            m.put("location", r.getLocation());
            return m;
        }).collect(Collectors.toList());
        return objectMapper.writeValueAsString(result);
    }

    private String handleCheckAvailability(JsonNode input) throws Exception {
        String date = input.get("date").asText();
        return objectMapper.writeValueAsString(recordService.getAvailability(date));
    }

    private String handleCreateBooking(JsonNode input, UUID userId) throws Exception {
        UUID roomId  = UUID.fromString(input.get("room_id").asText());
        String title = input.has("title") ? input.get("title").asText() : "預約";
        OffsetDateTime start = OffsetDateTime.parse(input.get("started_time").asText());
        OffsetDateTime end   = OffsetDateTime.parse(input.get("ended_time").asText());

        RecordDTO dto = RecordDTO.builder()
            .roomId(roomId)
            .title(title)
            .startedTime(start)
            .endedTime(end)
            .build();

        if (input.has("reminder_minutes") && !input.get("reminder_minutes").isNull()) {
            int mins = input.get("reminder_minutes").asInt();
            dto.setReminderTime(start.minusMinutes(mins));
        }

        RecordVO record = recordService.createRecord(dto, userId);
        return "預約建立成功！預約 ID：" + record.getRecordId()
            + "，時間：" + start + " ~ " + end;
    }

    private String handleListMyBookings(JsonNode input, UUID userId) throws Exception {
        boolean upcomingOnly = !input.has("upcoming_only") || input.get("upcoming_only").asBoolean(true);
        List<RecordVO> records = recordService.getMyRecords(userId);

        if (upcomingOnly) {
            OffsetDateTime now = OffsetDateTime.now();
            records = records.stream()
                .filter(r -> r.getStartedTime() != null && r.getStartedTime().isAfter(now))
                .collect(Collectors.toList());
        }

        List<Map<String, Object>> result = records.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("recordId",   r.getRecordId());
            m.put("title",      r.getTitle());
            m.put("roomId",     r.getRoomId());
            m.put("startedTime", r.getStartedTime());
            m.put("endedTime",   r.getEndedTime());
            m.put("status",     r.getStatus() == 1 ? "confirmed" : "cancelled");
            return m;
        }).collect(Collectors.toList());

        return objectMapper.writeValueAsString(result);
    }

    private String handleCancelBooking(JsonNode input, UUID userId) {
        UUID recordId = UUID.fromString(input.get("record_id").asText());
        recordService.cancelRecord(recordId, userId, false);
        return "預約 " + recordId + " 已成功取消。";
    }
}
