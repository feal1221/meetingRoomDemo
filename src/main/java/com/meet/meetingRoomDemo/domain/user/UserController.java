package com.meet.meetingRoomDemo.domain.user;

import com.meet.meetingRoomDemo.handler.Result;
import com.meet.meetingRoomDemo.responseBody.RespBodySkeleton;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name= "User Management",description = "用戶管理API")
@RestController
public class UserController {

    @Autowired
    private UserService userService;


    @Operation(summary = "新增用戶", description = "新增用戶")
    @PostMapping("/users/create")
    public Result<UserVO> createUser(@Valid @RequestBody UserVO userVo)  {
        UserVO newUser = userService.createUser(userVo);
        return Result.success(newUser);
    }

    @Operation(summary = "更新用戶", description = "更新用戶")
    @PostMapping("/users/update")
    public Result<UserVO> updateUser(@Valid @RequestBody UserVO userVo) {
        if (userVo.getUserId() == null) {
            return Result.error(500, "userId is required for update");
        }
        UserVO updateUser = userService.updateUser(userVo);
        return Result.success(updateUser);
    }

    @Operation(summary = "查詢用戶", description = "查詢用戶")
    @PostMapping("/users/all")
    public java.util.List<UserVO> getAllUsers() {
        return userService.getAllUsers();
    }

    @Operation(summary = "依ID查詢用戶", description = "依ID查詢用戶")
    @PostMapping("/users/id")
    public UserVO getUserById(String userId) {
        return userService.getUserById(userId);
    }

    @Operation(summary = "檢查Email是否存在", description = "檢查Email是否存在")
    @PostMapping("/users/checkEmail")
    public Boolean isEmailExists(String email) {
        return userService.isEmailExists(email);
    }


}
