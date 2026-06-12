package com.meet.meetingRoomDemo.domain.record.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchBookingResponse {

    private int count;

    /** 週期預約的系列 ID（批次預約此欄為 null） */
    private UUID parentRecordId;

    private List<UUID> recordIds;
}
