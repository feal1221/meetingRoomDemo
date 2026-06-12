package com.meet.meetingRoomDemo.domain.record;

import com.meet.meetingRoomDemo.auth.UserPrincipal;
import com.meet.meetingRoomDemo.domain.record.dto.BatchBookingResponse;
import com.meet.meetingRoomDemo.handler.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Booking Records", description = "預約記錄API")
@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    // ─── 單筆預約 ─────────────────────────────────────────────────────────────

    @Operation(summary = "建立單筆預約")
    @PostMapping
    public Result<RecordVO> createRecord(@Valid @RequestBody RecordDTO dto,
                                         Authentication authentication) {
        UserPrincipal p = (UserPrincipal) authentication.getPrincipal();
        return Result.success(recordService.createRecord(dto, p.getUserId()));
    }

    // ─── 批次預約 ─────────────────────────────────────────────────────────────

    @Operation(summary = "批次預約（一次送多個時段）",
               description = "送出 List<RecordDTO>，所有時段全部通過衝突檢查才會寫入；任一衝突則全部失敗。")
    @PostMapping("/batch")
    public Result<BatchBookingResponse> createBatch(@RequestBody List<RecordDTO> dtos,
                                                    Authentication authentication) {
        UserPrincipal p = (UserPrincipal) authentication.getPrincipal();
        return Result.success(recordService.createBatch(dtos, p.getUserId()));
    }

    // ─── 週期預約 ─────────────────────────────────────────────────────────────

    @Operation(summary = "週期預約（RRULE 展開，上限 52 筆）",
               description = """
                   rrule 範例：
                   - 每週一共10次：FREQ=WEEKLY;BYDAY=MO;COUNT=10
                   - 每日共5天：FREQ=DAILY;COUNT=5
                   - 每月共6次：FREQ=MONTHLY;COUNT=6
                   - 每週一三五直到年底：FREQ=WEEKLY;BYDAY=MO,WE,FR;UNTIL=20241231T000000Z
                   """)
    @PostMapping("/recurring")
    public Result<BatchBookingResponse> createRecurring(@Valid @RequestBody RecordDTO dto,
                                                        Authentication authentication) {
        UserPrincipal p = (UserPrincipal) authentication.getPrincipal();
        return Result.success(recordService.createRecurring(dto, p.getUserId()));
    }

    // ─── 查詢 ─────────────────────────────────────────────────────────────────

    @Operation(summary = "查詢我的預約（依建立時間降冪）")
    @GetMapping("/my")
    public Result<List<RecordVO>> getMyRecords(Authentication authentication) {
        UserPrincipal p = (UserPrincipal) authentication.getPrincipal();
        return Result.success(recordService.getMyRecords(p.getUserId()));
    }

    @Operation(summary = "查詢預約詳情")
    @GetMapping("/{id}")
    public Result<RecordVO> getRecordById(@PathVariable UUID id) {
        RecordVO record = recordService.getRecordById(id);
        if (record == null) return Result.error(404, "Record not found");
        return Result.success(record);
    }

    // ─── 取消 ─────────────────────────────────────────────────────────────────

    @Operation(summary = "取消單筆預約（本人或管理員）")
    @PutMapping("/{id}/cancel")
    public Result<String> cancelRecord(@PathVariable UUID id, Authentication authentication) {
        UserPrincipal p = (UserPrincipal) authentication.getPrincipal();
        recordService.cancelRecord(id, p.getUserId(), isAdmin(p));
        return Result.success("Booking cancelled successfully");
    }

    @Operation(summary = "取消整個週期系列（本人或管理員）",
               description = "提供系列中任一筆 recordId，即可取消整個 parentRecordId 關聯的所有未取消預約。")
    @PutMapping("/{id}/cancel-series")
    public Result<String> cancelSeries(@PathVariable UUID id, Authentication authentication) {
        UserPrincipal p = (UserPrincipal) authentication.getPrincipal();
        int count = recordService.cancelSeries(id, p.getUserId(), isAdmin(p));
        return Result.success("Cancelled " + count + " bookings in the series");
    }

    private boolean isAdmin(UserPrincipal p) {
        return p.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
