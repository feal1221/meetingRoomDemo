package com.meet.meetingRoomDemo.domain.record;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class RecordDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "record_id")
    private UUID recordId;

    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "user_id")
    private UUID userId;
    private String reason;

    @Column(name = "comment_text", length = 255)
    private String commentText;
    private Integer status;

    @CreatedBy
    @Column(name = "created_by")
    private String createdBy;

    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_time",insertable=false,updatable=false)
    private OffsetDateTime createdTime;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    @LastModifiedDate
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_time",insertable=false,updatable=false)
    private OffsetDateTime  updatedTime;

    @Column(name = "started_time")
    private OffsetDateTime  startedTime;

    @Column(name = "ended_time")
    private OffsetDateTime  endedTime;

    @Column(name = "reminder_time")
    private OffsetDateTime  reminderTime;

    @Column(name = "is_notified")
    private Integer isNotified;


}
