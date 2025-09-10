package com.meet.meetingRoomDemo.domain.record;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String recordId;
    private String roomId;
    private String userId;
    private String reason;
    private String commentText;
    private Integer status;
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime  updatedAt;
    private OffsetDateTime  startTime;
    private OffsetDateTime  endTime;
    private OffsetDateTime  remindTime;
    private Integer isNotified;


}
