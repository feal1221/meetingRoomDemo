package com.meet.meetingRoomDemo.domain.record;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordDTO {

    @NotNull(message = "roomId is required")
    private UUID roomId;

    private String title;

    private String reason;

    @NotNull(message = "startedTime is required")
    private OffsetDateTime startedTime;

    @NotNull(message = "endedTime is required")
    private OffsetDateTime endedTime;

    private OffsetDateTime reminderTime;

    private String rrule;
}
