package com.meet.meetingRoomDemo.ai;

import com.meet.meetingRoomDemo.ai.dto.ChatRequest;
import com.meet.meetingRoomDemo.ai.dto.ChatResponse;
import com.meet.meetingRoomDemo.auth.UserPrincipal;
import com.meet.meetingRoomDemo.handler.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI Chat", description = "AI 聊天室預約（Claude Tool Use）")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(
        summary = "傳送訊息給 AI 助理",
        description = """
            對話範例：
            - "有哪些會議室？"
            - "幫我查明天 A 會議室還有哪些時段可以預約"
            - "幫我預約後天早上九點到十點的 A 會議室，主題是週會"
            - "我有哪些預約？"
            - "幫我取消這個預約：{recordId}"

            對話歷史存於 Redis，30 分鐘無操作後自動清除。
            """
    )
    @PostMapping("/send")
    public Result<ChatResponse> send(@Valid @RequestBody ChatRequest request,
                                     Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String reply = chatService.chat(request.getMessage(), principal.getUserId());
        return Result.success(new ChatResponse(reply));
    }

    @Operation(summary = "清除對話歷史")
    @DeleteMapping("/history")
    public Result<String> clearHistory(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        chatService.clearHistory(principal.getUserId());
        return Result.success("Conversation history cleared");
    }
}
