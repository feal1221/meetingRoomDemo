package com.meet.meetingRoomDemo.domain.record;

import com.meet.meetingRoomDemo.auth.UserPrincipal;
import com.meet.meetingRoomDemo.domain.record.dto.BatchBookingResponse;
import com.meet.meetingRoomDemo.domain.record.dto.MyRecordResponse;
import com.meet.meetingRoomDemo.domain.record.dto.RecurringBookingRequest;
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

    @Operation(summary = "批次預約（指定日期清單 + 每日時段）",
               description = "傳入日期清單與每日開始/結束時間，所有時段全部通過衝突檢查才會寫入；任一衝突則全部失敗。")
    @PostMapping("/batch")
    public Result<BatchBookingResponse> createBatch(@Valid @RequestBody RecurringBookingRequest req,
                                                    Authentication authentication) {
        UserPrincipal p = (UserPrincipal) authentication.getPrincipal();
        return Result.success(recordService.createBatch(req, p.getUserId()));
    }

    // ─── 週期預約 ─────────────────────────────────────────────────────────────

    @Operation(summary = "週期預約（指定日期清單 + 每日時段）",
               description = """
                   傳入日期清單與每日開始/結束時間，系統逐一檢查衝突後批次建立預約。
                   日期格式：yyyy-MM-dd，時間格式：HH:mm（Asia/Taipei）
                   """)
    @PostMapping("/recurring")
    public Result<BatchBookingResponse> createRecurring(@Valid @RequestBody RecurringBookingRequest req,
                                                        Authentication authentication) {
        UserPrincipal p = (UserPrincipal) authentication.getPrincipal();
        return Result.success(recordService.createRecurring(req, p.getUserId()));
    }

    // ─── 查詢 ─────────────────────────────────────────────────────────────────

    @Operation(summary = "查詢我的預約（依建立時間降冪）")
    @GetMapping("/my")
    public Result<List<MyRecordResponse>> getMyRecords(Authentication authentication) {
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
