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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "t_record")
@Entity
public class RecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "record_id")
    private UUID recordId;

    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title")
    private String title;

    private String reason;

    @Column(name = "comment_text", length = 255)
    private String commentText;

    private Integer status;

    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "parent_record_id")
    private UUID parentRecordId;

    @Column(name = "rrule")
    private String rrule;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_time", updatable = false)
    private OffsetDateTime createdTime;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    @LastModifiedDate
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_time")
    private OffsetDateTime updatedTime;

    @Column(name = "started_time")
    private OffsetDateTime startedTime;

    @Column(name = "ended_time")
    private OffsetDateTime endedTime;

    @Column(name = "reminder_time")
    private OffsetDateTime reminderTime;

    @Column(name = "is_notified")
    private Integer isNotified;
}
