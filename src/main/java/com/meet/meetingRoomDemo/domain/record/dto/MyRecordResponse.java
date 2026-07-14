package com.meet.meetingRoomDemo.domain.record.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class MyRecordResponse {

    private UUID recordId;
    private UUID roomId;
    private String roomName;
    private UUID userId;
    private String title;
    private String reason;
    private String commentText;
    private Integer status;
    private UUID parentRecordId;
    private String rrule;
    private OffsetDateTime startedTime;
    private OffsetDateTime endedTime;
    private OffsetDateTime reminderTime;
    private Integer isNotified;
    private String createdBy;
    private OffsetDateTime createdTime;
    private String updatedBy;
    private OffsetDateTime updatedTime;
}
