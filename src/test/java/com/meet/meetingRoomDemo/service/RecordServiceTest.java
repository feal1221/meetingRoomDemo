package com.meet.meetingRoomDemo.service;

import com.meet.meetingRoomDemo.domain.record.RecordDTO;
import com.meet.meetingRoomDemo.domain.record.RecordRepository;
import com.meet.meetingRoomDemo.domain.record.RecordService;
import com.meet.meetingRoomDemo.domain.record.RecordVO;
import com.meet.meetingRoomDemo.domain.record.dto.BatchBookingResponse;
import com.meet.meetingRoomDemo.domain.room.RoomRepository;
import com.meet.meetingRoomDemo.domain.room.RoomVO;
import com.meet.meetingRoomDemo.handler.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordServiceTest {

    @Mock private RecordRepository recordRepository;
    @Mock private RoomRepository   roomRepository;

    @InjectMocks
    private RecordService recordService;

    private UUID userId;
    private UUID roomId;
    private RoomVO activeRoom;

    /** 明天 09:00+08:00 */
    private OffsetDateTime tomorrowMorning;
    /** 明天 10:00+08:00 */
    private OffsetDateTime tomorrowNoon;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        roomId = UUID.randomUUID();
        activeRoom = RoomVO.builder()
            .roomId(roomId).roomName("A Room").status(1).capacity(10).build();

        ZoneOffset taipei = ZoneOffset.ofHours(8);
        tomorrowMorning = OffsetDateTime.now(taipei).plusDays(1)
            .withHour(9).withMinute(0).withSecond(0).withNano(0);
        tomorrowNoon = tomorrowMorning.plusHours(1);
    }

    // ─── createRecord — 正常路徑 ──────────────────────────────────────────────

    @Test
    void createRecord_validInput_savesAndReturnsRecord() {
        RecordDTO dto = buildDto(roomId, tomorrowMorning, tomorrowNoon);
        RecordVO saved = buildRecord(UUID.randomUUID(), roomId, userId, 1);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(activeRoom));
        when(recordRepository.findConflicts(eq(roomId), any(), any())).thenReturn(List.of());
        when(recordRepository.save(any())).thenReturn(saved);

        RecordVO result = recordService.createRecord(dto, userId);

        assertNotNull(result);
        assertEquals(1, result.getStatus());
        verify(recordRepository).save(any(RecordVO.class));
    }

    // ─── createRecord — 時間驗證 ──────────────────────────────────────────────

    @Test
    void createRecord_endBeforeStart_throwsIllegalArgument() {
        RecordDTO dto = buildDto(roomId, tomorrowNoon, tomorrowMorning); // 結束在開始之前

        assertThrows(IllegalArgumentException.class,
            () -> recordService.createRecord(dto, userId));
        verifyNoInteractions(roomRepository, recordRepository);
    }

    @Test
    void createRecord_startInPast_throwsIllegalArgument() {
        OffsetDateTime pastStart = OffsetDateTime.now().minusHours(2);
        OffsetDateTime pastEnd   = OffsetDateTime.now().minusHours(1);
        RecordDTO dto = buildDto(roomId, pastStart, pastEnd);

        assertThrows(IllegalArgumentException.class,
            () -> recordService.createRecord(dto, userId));
    }

    // ─── createRecord — 房間驗證 ──────────────────────────────────────────────

    @Test
    void createRecord_roomNotFound_throwsIllegalArgument() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> recordService.createRecord(buildDto(roomId, tomorrowMorning, tomorrowNoon), userId));
    }

    @Test
    void createRecord_inactiveRoom_throwsIllegalArgument() {
        RoomVO inactive = RoomVO.builder().roomId(roomId).roomName("A Room").status(0).capacity(10).build();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(inactive));

        assertThrows(IllegalArgumentException.class,
            () -> recordService.createRecord(buildDto(roomId, tomorrowMorning, tomorrowNoon), userId));
    }

    // ─── createRecord — 衝突偵測 ──────────────────────────────────────────────

    @Test
    void createRecord_conflictExists_throwsConflictException() {
        RecordVO conflicting = buildRecord(UUID.randomUUID(), roomId, UUID.randomUUID(), 1);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(activeRoom));
        when(recordRepository.findConflicts(eq(roomId), any(), any()))
            .thenReturn(List.of(conflicting));

        assertThrows(ConflictException.class,
            () -> recordService.createRecord(buildDto(roomId, tomorrowMorning, tomorrowNoon), userId));
        verify(recordRepository, never()).save(any());
    }

    // ─── cancelRecord ─────────────────────────────────────────────────────────

    @Test
    void cancelRecord_owner_setsStatusCancelled() {
        RecordVO record = buildRecord(UUID.randomUUID(), roomId, userId, 1);
        when(recordRepository.findById(record.getRecordId())).thenReturn(Optional.of(record));
        when(recordRepository.save(any())).thenReturn(record);

        recordService.cancelRecord(record.getRecordId(), userId, false);

        verify(recordRepository).save(argThat(r -> r.getStatus() == 2));
    }

    @Test
    void cancelRecord_adminCancelsOtherUser_succeeds() {
        UUID otherUser = UUID.randomUUID();
        RecordVO record = buildRecord(UUID.randomUUID(), roomId, otherUser, 1);
        when(recordRepository.findById(record.getRecordId())).thenReturn(Optional.of(record));
        when(recordRepository.save(any())).thenReturn(record);

        // isAdmin = true → should succeed even though userId != record.userId
        recordService.cancelRecord(record.getRecordId(), userId, true);

        verify(recordRepository).save(argThat(r -> r.getStatus() == 2));
    }

    @Test
    void cancelRecord_nonOwner_throwsAccessDeniedException() {
        UUID otherUser = UUID.randomUUID();
        RecordVO record = buildRecord(UUID.randomUUID(), roomId, otherUser, 1);
        when(recordRepository.findById(record.getRecordId())).thenReturn(Optional.of(record));

        assertThrows(AccessDeniedException.class,
            () -> recordService.cancelRecord(record.getRecordId(), userId, false));
    }

    @Test
    void cancelRecord_alreadyCancelled_throwsIllegalArgument() {
        RecordVO cancelled = buildRecord(UUID.randomUUID(), roomId, userId, 2);
        when(recordRepository.findById(cancelled.getRecordId())).thenReturn(Optional.of(cancelled));

        assertThrows(IllegalArgumentException.class,
            () -> recordService.cancelRecord(cancelled.getRecordId(), userId, false));
    }

    @Test
    void cancelRecord_notFound_throwsIllegalArgument() {
        UUID missingId = UUID.randomUUID();
        when(recordRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> recordService.cancelRecord(missingId, userId, false));
    }

    // ─── cancelSeries ─────────────────────────────────────────────────────────

    @Test
    void cancelSeries_owner_cancelsAllSeriesRecords() {
        UUID parentId = UUID.randomUUID();
        RecordVO r1 = buildRecord(UUID.randomUUID(), roomId, userId, 1);
        r1.setParentRecordId(parentId);
        RecordVO r2 = buildRecord(UUID.randomUUID(), roomId, userId, 1);
        r2.setParentRecordId(parentId);

        when(recordRepository.findById(r1.getRecordId())).thenReturn(Optional.of(r1));
        when(recordRepository.findByParentRecordIdAndStatusNot(parentId, 2))
            .thenReturn(List.of(r1, r2));
        when(recordRepository.saveAll(anyList())).thenReturn(List.of());

        int count = recordService.cancelSeries(r1.getRecordId(), userId, false);

        assertEquals(2, count);
        verify(recordRepository).saveAll(argThat(list ->
            ((List<RecordVO>) list).stream().allMatch(r -> r.getStatus() == 2)));
    }

    @Test
    void cancelSeries_notRecurring_throwsIllegalArgument() {
        RecordVO standalone = buildRecord(UUID.randomUUID(), roomId, userId, 1);
        // parentRecordId is null → not a recurring booking

        when(recordRepository.findById(standalone.getRecordId())).thenReturn(Optional.of(standalone));

        assertThrows(IllegalArgumentException.class,
            () -> recordService.cancelSeries(standalone.getRecordId(), userId, false));
    }

    // ─── createBatch ──────────────────────────────────────────────────────────

    @Test
    void createBatch_noConflicts_savesAll() {
        OffsetDateTime slot1Start = tomorrowMorning;
        OffsetDateTime slot1End   = tomorrowMorning.plusHours(1);
        OffsetDateTime slot2Start = tomorrowMorning.plusHours(2); // 不重疊
        OffsetDateTime slot2End   = tomorrowMorning.plusHours(3);

        List<RecordDTO> dtos = List.of(
            buildDto(roomId, slot1Start, slot1End),
            buildDto(roomId, slot2Start, slot2End)
        );

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(activeRoom));
        when(recordRepository.findConflicts(eq(roomId), any(), any())).thenReturn(List.of());
        when(recordRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<RecordVO> records = new ArrayList<>((List<RecordVO>) inv.getArgument(0));
            records.forEach(r -> r.setRecordId(UUID.randomUUID()));
            return records;
        });

        BatchBookingResponse response = recordService.createBatch(dtos, userId);

        assertEquals(2, response.getCount());
        assertNull(response.getParentRecordId()); // batch 無 parentRecordId
    }

    @Test
    void createBatch_internalOverlap_throwsConflictException() {
        OffsetDateTime start = tomorrowMorning;
        List<RecordDTO> dtos = List.of(
            buildDto(roomId, start, start.plusHours(1)),
            buildDto(roomId, start.plusMinutes(30), start.plusHours(2)) // 與第一筆重疊
        );

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(activeRoom));
        when(recordRepository.findConflicts(eq(roomId), any(), any())).thenReturn(List.of());

        assertThrows(ConflictException.class, () -> recordService.createBatch(dtos, userId));
    }

    @Test
    void createBatch_emptyList_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
            () -> recordService.createBatch(List.of(), userId));
    }

    // ─── createRecurring ──────────────────────────────────────────────────────

    @Test
    void createRecurring_weeklyMonday_count3_creates3Records() {
        // 取得下一個星期一（確保在未來）
        OffsetDateTime nextMonday = OffsetDateTime.now(ZoneOffset.ofHours(8))
            .plusWeeks(1)
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
            .withHour(9).withMinute(0).withSecond(0).withNano(0);

        RecordDTO dto = RecordDTO.builder()
            .roomId(roomId)
            .title("Weekly Standup")
            .startedTime(nextMonday)
            .endedTime(nextMonday.plusHours(1))
            .rrule("FREQ=WEEKLY;BYDAY=MO;COUNT=3")
            .build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(activeRoom));
        when(recordRepository.findConflicts(eq(roomId), any(), any())).thenReturn(List.of());
        when(recordRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<RecordVO> records = new ArrayList<>((List<RecordVO>) inv.getArgument(0));
            records.forEach(r -> r.setRecordId(UUID.randomUUID()));
            return records;
        });

        BatchBookingResponse response = recordService.createRecurring(dto, userId);

        assertEquals(3, response.getCount());
        assertNotNull(response.getParentRecordId());
        // 驗證所有筆都用相同 parentRecordId
        verify(recordRepository).saveAll(argThat(list ->
            ((List<RecordVO>) list).stream()
                .allMatch(r -> response.getParentRecordId().equals(r.getParentRecordId()))));
    }

    @Test
    void createRecurring_missingRrule_throwsIllegalArgument() {
        RecordDTO dto = buildDto(roomId, tomorrowMorning, tomorrowNoon);
        // rrule is null

        assertThrows(IllegalArgumentException.class,
            () -> recordService.createRecurring(dto, userId));
    }

    @Test
    void createRecurring_conflictOnOccurrence_throwsConflictException() {
        OffsetDateTime nextMonday = OffsetDateTime.now(ZoneOffset.ofHours(8))
            .plusWeeks(1)
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
            .withHour(9).withMinute(0).withSecond(0).withNano(0);

        RecordDTO dto = RecordDTO.builder()
            .roomId(roomId)
            .title("Meeting")
            .startedTime(nextMonday)
            .endedTime(nextMonday.plusHours(1))
            .rrule("FREQ=WEEKLY;BYDAY=MO;COUNT=3")
            .build();

        RecordVO conflicting = buildRecord(UUID.randomUUID(), roomId, UUID.randomUUID(), 1);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(activeRoom));
        // 第一次衝突偵測失敗
        when(recordRepository.findConflicts(eq(roomId), any(), any()))
            .thenReturn(List.of(conflicting));

        assertThrows(ConflictException.class,
            () -> recordService.createRecurring(dto, userId));
    }

    // ─── 輔助方法 ─────────────────────────────────────────────────────────────

    private RecordDTO buildDto(UUID room, OffsetDateTime start, OffsetDateTime end) {
        return RecordDTO.builder()
            .roomId(room)
            .title("Test Booking")
            .startedTime(start)
            .endedTime(end)
            .build();
    }

    private RecordVO buildRecord(UUID id, UUID room, UUID user, int status) {
        return RecordVO.builder()
            .recordId(id)
            .roomId(room)
            .userId(user)
            .title("Test")
            .startedTime(tomorrowMorning)
            .endedTime(tomorrowNoon)
            .status(status)
            .isNotified(0)
            .build();
    }
}
