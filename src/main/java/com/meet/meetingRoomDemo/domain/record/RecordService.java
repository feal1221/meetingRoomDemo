package com.meet.meetingRoomDemo.domain.record;

import com.meet.meetingRoomDemo.domain.record.dto.AvailabilityDTO;
import com.meet.meetingRoomDemo.domain.record.dto.BatchBookingResponse;
import com.meet.meetingRoomDemo.domain.record.dto.MyRecordResponse;
import com.meet.meetingRoomDemo.domain.record.dto.RecurringBookingRequest;
import com.meet.meetingRoomDemo.domain.room.RoomRepository;
import com.meet.meetingRoomDemo.domain.room.RoomVO;
import com.meet.meetingRoomDemo.handler.ConflictException;
import com.meet.meetingRoomDemo.util.RRuleExpander;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final RecordRepository recordRepository;
    private final RoomRepository roomRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // 單筆預約
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public RecordVO createRecord(RecordDTO dto, UUID userId) {
        validateTime(dto.getStartedTime(), dto.getEndedTime());
        RoomVO room = requireActiveRoom(dto.getRoomId());
        checkConflict(dto.getRoomId(), dto.getStartedTime(), dto.getEndedTime(), room.getRoomName());
        return recordRepository.save(buildRecord(dto, userId, null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 批次預約（一次送多個時段）
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public BatchBookingResponse createBatch(RecurringBookingRequest req, UUID userId) {
        RoomVO room = requireActiveRoom(req.getRoomId());

        ZoneOffset taipei = ZoneOffset.ofHours(8);
        LocalTime startLT = LocalTime.parse(req.getStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime endLT   = LocalTime.parse(req.getEndTime(),   DateTimeFormatter.ofPattern("HH:mm"));

        if (!startLT.isBefore(endLT)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }

        List<RecordVO> toSave = new ArrayList<>();

        for (String dateStr : req.getDates()) {
            LocalDate date = LocalDate.parse(dateStr);
            OffsetDateTime start = date.atTime(startLT).atOffset(taipei);
            OffsetDateTime end   = date.atTime(endLT).atOffset(taipei);

            if (start.toInstant().isBefore(Instant.now())) {
                throw new IllegalArgumentException("Cannot create a booking in the past: " + dateStr);
            }
            checkConflict(req.getRoomId(), start, end, room.getRoomName());

            // 批次內部衝突檢查
            boolean internalConflict = toSave.stream()
                .anyMatch(r -> r.getEndedTime().toInstant().isAfter(start.toInstant())
                            && r.getStartedTime().toInstant().isBefore(end.toInstant()));
            if (internalConflict) {
                throw new ConflictException("Conflicting slots within the batch: " + dateStr);
            }

            toSave.add(RecordVO.builder()
                .roomId(req.getRoomId())
                .userId(userId)
                .title(req.getTitle())
                .reason(req.getReason())
                .startedTime(start)
                .endedTime(end)
                .status(1)
                .isNotified(0)
                .build());
        }

        List<RecordVO> saved = recordRepository.saveAll(toSave);
        return BatchBookingResponse.builder()
            .count(saved.size())
            .recordIds(saved.stream().map(RecordVO::getRecordId).collect(Collectors.toList()))
            .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 週期預約（RRULE 展開）
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public BatchBookingResponse createRecurring(RecurringBookingRequest req, UUID userId) {
        RoomVO room = requireActiveRoom(req.getRoomId());

        ZoneOffset taipei = ZoneOffset.ofHours(8);
        LocalTime startLT = LocalTime.parse(req.getStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime endLT   = LocalTime.parse(req.getEndTime(),   DateTimeFormatter.ofPattern("HH:mm"));

        if (!startLT.isBefore(endLT)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }

        UUID parentId = UUID.randomUUID();
        List<RecordVO> toSave = new ArrayList<>();

        for (String dateStr : req.getDates()) {
            LocalDate date = LocalDate.parse(dateStr);
            OffsetDateTime start = date.atTime(startLT).atOffset(taipei);
            OffsetDateTime end   = date.atTime(endLT).atOffset(taipei);

            if (start.toInstant().isBefore(Instant.now())) {
                throw new IllegalArgumentException("Cannot create a booking in the past: " + dateStr);
            }
            checkConflict(req.getRoomId(), start, end, room.getRoomName());

            toSave.add(RecordVO.builder()
                .roomId(req.getRoomId())
                .userId(userId)
                .title(req.getTitle())
                .reason(req.getReason())
                .startedTime(start)
                .endedTime(end)
                .parentRecordId(parentId)
                .status(1)
                .isNotified(0)
                .build());
        }

        List<RecordVO> saved = recordRepository.saveAll(toSave);
        return BatchBookingResponse.builder()
            .count(saved.size())
            .parentRecordId(parentId)
            .recordIds(saved.stream().map(RecordVO::getRecordId).collect(Collectors.toList()))
            .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 查詢
    // ─────────────────────────────────────────────────────────────────────────

    public List<MyRecordResponse> getMyRecords(UUID userId) {
        List<RecordVO> records = recordRepository.findByUserIdOrderByStartedTimeDesc(userId);

        Set<UUID> roomIds = records.stream().map(RecordVO::getRoomId).collect(Collectors.toSet());
        Map<UUID, String> roomNameMap = roomRepository.findAllById(roomIds).stream()
            .collect(Collectors.toMap(RoomVO::getRoomId, RoomVO::getRoomName));

        return records.stream()
            .map(r -> MyRecordResponse.builder()
                .recordId(r.getRecordId())
                .roomId(r.getRoomId())
                .roomName(roomNameMap.get(r.getRoomId()))
                .userId(r.getUserId())
                .title(r.getTitle())
                .reason(r.getReason())
                .commentText(r.getCommentText())
                .status(r.getStatus())
                .parentRecordId(r.getParentRecordId())
                .rrule(r.getRrule())
                .startedTime(r.getStartedTime())
                .endedTime(r.getEndedTime())
                .reminderTime(r.getReminderTime())
                .isNotified(r.getIsNotified())
                .createdBy(r.getCreatedBy())
                .createdTime(r.getCreatedTime())
                .updatedBy(r.getUpdatedBy())
                .updatedTime(r.getUpdatedTime())
                .build())
            .collect(Collectors.toList());
    }

    public RecordVO getRecordById(UUID recordId) {
        return recordRepository.findById(recordId).orElse(null);
    }

    public List<RecordVO> getRoomSlots(UUID roomId, LocalDate date) {
        ZoneOffset taipei = ZoneOffset.ofHours(8);
        OffsetDateTime start = date.atStartOfDay().atOffset(taipei);
        OffsetDateTime end = date.plusDays(1).atStartOfDay().atOffset(taipei);
        return recordRepository.findByRoomIdAndDate(roomId, start, end);
    }

    public AvailabilityDTO getAvailability(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        ZoneOffset taipei = ZoneOffset.ofHours(8);
        OffsetDateTime start = date.atStartOfDay().atOffset(taipei);
        OffsetDateTime end = date.plusDays(1).atStartOfDay().atOffset(taipei);

        List<RoomVO> rooms = roomRepository.findByStatus(1);
        List<RecordVO> records = recordRepository.findAllByDate(start, end);

        Map<UUID, List<RecordVO>> byRoom = records.stream()
            .collect(Collectors.groupingBy(RecordVO::getRoomId));

        List<AvailabilityDTO.RoomSlots> roomSlots = rooms.stream()
            .map(room -> AvailabilityDTO.RoomSlots.builder()
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .capacity(room.getCapacity())
                .bookedSlots(
                    byRoom.getOrDefault(room.getRoomId(), List.of()).stream()
                        .map(r -> AvailabilityDTO.BookedSlot.builder()
                            .recordId(r.getRecordId())
                            .title(r.getTitle())
                            .createdBy(r.getCreatedBy())
                            .startedTime(r.getStartedTime())
                            .endedTime(r.getEndedTime())
                            .build())
                        .collect(Collectors.toList())
                )
                .build())
            .collect(Collectors.toList());

        return AvailabilityDTO.builder()
            .date(dateStr)
            .rooms(roomSlots)
            .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 取消
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void cancelRecord(UUID recordId, UUID userId, boolean isAdmin) {
        RecordVO record = recordRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Record not found: " + recordId));
        if (!isAdmin && !record.getUserId().equals(userId)) {
            throw new AccessDeniedException("Cannot cancel another user's booking");
        }
        if (record.getStatus() == 2) {
            throw new IllegalArgumentException("Booking is already cancelled");
        }
        record.setStatus(2);
        recordRepository.save(record);
    }

    @Transactional
    public int cancelSeries(UUID recordId, UUID userId, boolean isAdmin) {
        RecordVO record = recordRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Record not found: " + recordId));

        UUID parentId = record.getParentRecordId();
        if (parentId == null) {
            throw new IllegalArgumentException("This booking is not part of a recurring series");
        }

        List<RecordVO> series = recordRepository.findByParentRecordIdAndStatusNot(parentId, 2);
        if (!isAdmin) {
            series.stream()
                .filter(r -> !r.getUserId().equals(userId))
                .findFirst()
                .ifPresent(r -> { throw new AccessDeniedException("Cannot cancel another user's recurring series"); });
        }
        series.forEach(r -> r.setStatus(2));
        recordRepository.saveAll(series);
        return series.size();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 內部輔助方法
    // ─────────────────────────────────────────────────────────────────────────

    private RoomVO requireActiveRoom(UUID roomId) {
        RoomVO room = roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        if (room.getStatus() != 1) {
            throw new IllegalArgumentException("Room is not available: " + room.getRoomName());
        }
        return room;
    }

    private void checkConflict(UUID roomId, OffsetDateTime start, OffsetDateTime end, String roomName) {
        if (!recordRepository.findConflicts(roomId, start, end).isEmpty()) {
            throw new ConflictException(
                "Time slot already booked for room '" + roomName + "' at " + start);
        }
    }

    private void validateTime(OffsetDateTime startedTime, OffsetDateTime endedTime) {
        if (!startedTime.toInstant().isBefore(endedTime.toInstant())) {
            throw new IllegalArgumentException("startedTime must be before endedTime");
        }
        if (startedTime.toInstant().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Cannot create a booking in the past");
        }
    }

    private void requireFields(RecordDTO dto, int index) {
        if (dto.getRoomId() == null)
            throw new IllegalArgumentException("roomId is required at batch index " + index);
        if (dto.getStartedTime() == null || dto.getEndedTime() == null)
            throw new IllegalArgumentException("startedTime and endedTime are required at batch index " + index);
    }

    private RecordVO buildRecord(RecordDTO dto, UUID userId, UUID parentId) {
        return RecordVO.builder()
            .roomId(dto.getRoomId())
            .userId(userId)
            .title(dto.getTitle())
            .reason(dto.getReason())
            .startedTime(dto.getStartedTime())
            .endedTime(dto.getEndedTime())
            .reminderTime(dto.getReminderTime())
            .rrule(dto.getRrule())
            .parentRecordId(parentId)
            .status(1)
            .isNotified(0)
            .build();
    }
}
