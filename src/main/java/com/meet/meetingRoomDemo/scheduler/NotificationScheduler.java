package com.meet.meetingRoomDemo.scheduler;

import com.meet.meetingRoomDemo.domain.record.RecordRepository;
import com.meet.meetingRoomDemo.domain.record.RecordVO;
import com.meet.meetingRoomDemo.domain.room.RoomRepository;
import com.meet.meetingRoomDemo.domain.room.RoomVO;
import com.meet.meetingRoomDemo.domain.user.UserRepository;
import com.meet.meetingRoomDemo.domain.user.UserVO;
import com.meet.meetingRoomDemo.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final RecordRepository recordRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final EmailService emailService;

    /**
     * 每 5 分鐘掃描一次：找到 reminder_time 已到且尚未發送的預約，發信後標記 is_notified=1。
     * 若發信失敗，保留 is_notified=0，下次排程自動重試。
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void sendPendingReminders() {
        List<RecordVO> pending = recordRepository.findPendingReminders(OffsetDateTime.now());
        if (pending.isEmpty()) return;

        log.info("Processing {} pending reminder(s)", pending.size());

        for (RecordVO record : pending) {
            try {
                UserVO user = userRepository.findById(record.getUserId()).orElse(null);
                RoomVO room = roomRepository.findById(record.getRoomId()).orElse(null);

                if (user == null || room == null) {
                    log.warn("Record {} has missing user or room — marking notified to skip", record.getRecordId());
                    markNotified(record);
                    continue;
                }

                emailService.sendBookingReminderEmail(
                    user.getEmail(),
                    user.getUserName(),
                    room.getRoomName(),
                    record.getTitle() != null ? record.getTitle() : "（無標題）",
                    record.getStartedTime(),
                    record.getEndedTime()
                );

                markNotified(record);

            } catch (Exception e) {
                log.error("Failed to send reminder for record {}: {}", record.getRecordId(), e.getMessage());
                // is_notified 保持 0，等下次排程重試
            }
        }
    }

    private void markNotified(RecordVO record) {
        record.setIsNotified(1);
        recordRepository.save(record);
    }
}
