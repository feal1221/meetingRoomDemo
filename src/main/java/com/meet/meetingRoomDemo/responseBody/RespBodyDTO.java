package com.meet.meetingRoomDemo.responseBody;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RespBodyDTO<T> {

    private RespBodySkeleton<T> body;

    public static <T> RespBodyDTO<T> getRespBodyDTO(T data) {
        return RespBodyDTO.<T>builder()
                .body(RespBodySkeleton.<T>builder()
                        .data(data)
                        .build())
                .build();
    }
}
