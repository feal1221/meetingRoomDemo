package com.meet.meetingRoomDemo.domain.record.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityDTO {

    private String date;
    private List<RoomSlots> rooms;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomSlots {
        private UUID roomId;
        private String roomName;
        private Integer capacity;
        private List<BookedSlot> bookedSlots;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookedSlot {
        private UUID recordId;
        private String title;
        private String createdBy;
        private OffsetDateTime startedTime;
        private OffsetDateTime endedTime;
    }
}
