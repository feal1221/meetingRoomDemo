package com.meet.meetingRoomDemo.domain.room;

import com.meet.meetingRoomDemo.domain.record.RecordService;
import com.meet.meetingRoomDemo.domain.record.RecordVO;
import com.meet.meetingRoomDemo.domain.record.dto.AvailabilityDTO;
import com.meet.meetingRoomDemo.handler.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Room Management", description = "會議室管理API")
@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final RecordService recordService;

    @Operation(summary = "查詢所有啟用中的會議室")
    @GetMapping
    public Result<List<RoomVO>> getAllRooms() {
        return Result.success(roomService.getAllActiveRooms());
    }

    @Operation(summary = "查詢單一會議室")
    @GetMapping("/{id}")
    public Result<RoomVO> getRoomById(@PathVariable UUID id) {
        RoomVO room = roomService.getRoomById(id);
        if (room == null) {
            return Result.error(404, "Room not found");
        }
        return Result.success(room);
    }

    @Operation(summary = "查詢所有房間當日可用性（行事曆全覽）",
               description = "date 格式: yyyy-MM-dd，例如 2024-01-15")
    @GetMapping("/availability")
    public Result<AvailabilityDTO> getAvailability(@RequestParam String date) {
        return Result.success(recordService.getAvailability(date));
    }

    @Operation(summary = "查詢指定房間某日的預約時段",
               description = "date 格式: yyyy-MM-dd，例如 2024-01-15")
    @GetMapping("/{id}/slots")
    public Result<List<RecordVO>> getRoomSlots(@PathVariable UUID id,
                                               @RequestParam String date) {
        return Result.success(recordService.getRoomSlots(id, LocalDate.parse(date)));
    }

    @Operation(summary = "新增會議室（管理員）")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<RoomVO> createRoom(@Valid @RequestBody RoomDTO dto) {
        return Result.success(roomService.createRoom(dto));
    }

    @Operation(summary = "更新會議室（管理員）")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<RoomVO> updateRoom(@PathVariable UUID id, @Valid @RequestBody RoomDTO dto) {
        return Result.success(roomService.updateRoom(id, dto));
    }

    @Operation(summary = "停用會議室（管理員，軟刪除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> deleteRoom(@PathVariable UUID id) {
        roomService.softDeleteRoom(id);
        return Result.success("Room disabled successfully");
    }
}
