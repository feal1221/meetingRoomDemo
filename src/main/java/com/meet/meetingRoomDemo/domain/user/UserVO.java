package com.meet.meetingRoomDemo.domain.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user")
@Entity
public class UserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String userId;
    private String userName;
    private String company;
    private String pwd;
    private Integer role;
    private String email;
    private Integer status;
    @CreatedBy
    private String createdBy;

    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    private OffsetDateTime createdAt;

    @LastModifiedBy
    private String updatedBy;

    @LastModifiedDate
    @Temporal(TemporalType.TIMESTAMP)
    private OffsetDateTime  updatedAt;
}
