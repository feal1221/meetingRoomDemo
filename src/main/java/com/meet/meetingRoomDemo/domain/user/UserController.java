package com.meet.meetingRoomDemo.domain.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name= "User Management",description = "用戶管理API")
@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "新增用戶", description = "新增用戶")
    @PostMapping("/users/create")
    public UserVO createUser(UserVO userVo) {
        return userService.createUser(userVo);
    }

    @Operation(summary = "更新用戶", description = "更新用戶")
    @PostMapping("/users/update")
    public UserVO updateUser(UserVO userVo) {
        return userService.updateUser(userVo);
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
