package com.meet.meetingRoomDemo.handler;

import com.meet.meetingRoomDemo.responseBody.RespBodySkeleton;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Result<T> {
    private int code;
    private String msg;
    private T data;
//    private RespBodySkeleton<T> body;

    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .msg("success")
                .data(data)
                .build();
    }

    public static <T> Result<T> error(int code, String msg) {
        return Result.<T>builder()
                .code(code)
                .msg(msg)
                .data(null)
                .build();
    }
}
