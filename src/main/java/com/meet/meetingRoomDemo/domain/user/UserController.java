package com.meet.meetingRoomDemo.domain.user;

import com.meet.meetingRoomDemo.handler.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "User Management", description = "用戶管理API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Operation(summary = "新增用戶")
    @PostMapping
    public Result<UserVO> createUser(@Valid @RequestBody UserVO userVo) {
        return Result.success(userService.createUser(userVo));
    }

    @Operation(summary = "更新用戶")
    @PutMapping
    public Result<UserVO> updateUser(@Valid @RequestBody UserVO userVo) {
        if (userVo.getUserId() == null) {
            return Result.error(400, "userId is required for update");
        }
        return Result.success(userService.updateUser(userVo));
    }

    @Operation(summary = "查詢所有用戶")
    @GetMapping
    public Result<List<UserVO>> getAllUsers() {
        return Result.success(userService.getAllUsers());
    }

    @Operation(summary = "依ID查詢用戶")
    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable UUID id) {
        UserVO user = userService.getUserById(id);
        if (user == null) {
            return Result.error(404, "User not found");
        }
        return Result.success(user);
    }

    @Operation(summary = "檢查Email是否存在")
    @GetMapping("/check-email")
    public Result<Boolean> isEmailExists(@RequestParam String email) {
        return Result.success(userService.isEmailExists(email));
    }
}
