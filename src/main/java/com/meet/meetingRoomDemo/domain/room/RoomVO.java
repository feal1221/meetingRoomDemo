package com.meet.meetingRoomDemo.domain.room;

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
public class RoomVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String roomId;
    private String roomName;
    private Integer status;
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime  updatedAt;
}
