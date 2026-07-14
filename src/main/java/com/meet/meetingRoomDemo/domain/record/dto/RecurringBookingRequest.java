package com.meet.meetingRoomDemo.domain.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class RecurringBookingRequest {

    @NotNull(message = "roomId is required")
    private UUID roomId;

    private String title;

    private String reason;

    @NotEmpty(message = "dates must not be empty")
    private List<String> dates;

    @NotBlank(message = "startTime is required")
    private String startTime;

    @NotBlank(message = "endTime is required")
    private String endTime;
}
