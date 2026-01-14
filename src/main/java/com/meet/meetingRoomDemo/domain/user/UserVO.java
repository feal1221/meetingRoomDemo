package com.meet.meetingRoomDemo.domain.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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
@Table(name = "user")
@Entity
public class UserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "id")
    private UUID userId;

    @Column(name = "user_name")
    @NotBlank(message = "userName is required")
    private String userName;

    @NotBlank(message = "company is required")
    private String company;

    @Size(min=8, max = 20, message = "pwd length must be less than or equal to 20,more than or equal to 8")
    @NotBlank(message = "pwd is required")
    private String pwd;

    @NotNull
    @Max(value = 1, message = "角色只有0或1")
    @Min(value = 0, message = "角色只有0或1")
    private Integer role;

    @Column(unique = true)
    @Email(message = "email format is invalid")
    @NotBlank(message = "email is required")
    private String email;
    @NotNull
    @Max(value = 1, message = "狀態只有0或1")
    @Min(value = 0, message = "狀態只有0或1")
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
}
