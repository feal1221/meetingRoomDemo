package com.meet.meetingRoomDemo.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 儲存在 Redis 的單筆對話記錄（僅保留最終文字，不含 tool_use 中間步驟）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationTurn {
    private String role;     // "user" | "assistant"
    private String content;
}
