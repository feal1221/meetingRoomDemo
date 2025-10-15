package com.meet.meetingRoomDemo.domain.room;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Entity
public class RoomVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private String roomId;
    private String roomName;
    private Integer status;
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime  updatedAt;
}
