package com.meet.meetingRoomDemo.domain.record;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RecordRepository extends JpaRepository<RecordVO, UUID>, JpaSpecificationExecutor<RecordVO> {

    // 衝突偵測：同一房間、時間重疊、非取消狀態
    @Query("SELECT r FROM RecordVO r WHERE r.roomId = :roomId AND r.status <> 2 " +
           "AND r.endedTime > :startedTime AND r.startedTime < :endedTime")
    List<RecordVO> findConflicts(@Param("roomId") UUID roomId,
                                 @Param("startedTime") OffsetDateTime startedTime,
                                 @Param("endedTime") OffsetDateTime endedTime);

    // 指定房間在某日期範圍內的預約（行事曆單房間視圖）
    @Query("SELECT r FROM RecordVO r WHERE r.roomId = :roomId AND r.status <> 2 " +
           "AND r.startedTime >= :start AND r.startedTime < :end ORDER BY r.startedTime")
    List<RecordVO> findByRoomIdAndDate(@Param("roomId") UUID roomId,
                                       @Param("start") OffsetDateTime start,
                                       @Param("end") OffsetDateTime end);

    // 所有房間在某日期範圍內的預約（行事曆全覽視圖）
    @Query("SELECT r FROM RecordVO r WHERE r.status <> 2 " +
           "AND r.startedTime >= :start AND r.startedTime < :end ORDER BY r.startedTime")
    List<RecordVO> findAllByDate(@Param("start") OffsetDateTime start,
                                  @Param("end") OffsetDateTime end);

    List<RecordVO> findByUserIdOrderByStartedTimeDesc(UUID userId);

    // 週期取消：找同系列中所有未取消的預約
    List<RecordVO> findByParentRecordIdAndStatusNot(UUID parentRecordId, Integer status);

    // 通知排程用：找到提醒時間已到但尚未發送的預約
    @Query("SELECT r FROM RecordVO r WHERE r.status = 1 AND r.isNotified = 0 " +
           "AND r.reminderTime IS NOT NULL AND r.reminderTime <= :now")
    List<RecordVO> findPendingReminders(@Param("now") OffsetDateTime now);
}
