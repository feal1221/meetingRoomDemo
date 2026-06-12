package com.meet.meetingRoomDemo.auth;

import com.meet.meetingRoomDemo.auth.dto.AuthResponse;
import com.meet.meetingRoomDemo.auth.dto.LoginRequest;
import com.meet.meetingRoomDemo.auth.dto.RegisterRequest;
import com.meet.meetingRoomDemo.domain.user.AuthProvider;
import com.meet.meetingRoomDemo.domain.user.UserService;
import com.meet.meetingRoomDemo.domain.user.UserVO;
import com.meet.meetingRoomDemo.handler.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Authentication", description = "認證API（註冊/登入/驗證Email/刷新Token）")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Operation(summary = "註冊帳號（個人 Email）")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<String> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success("Registration successful. Please check your email to verify your account.");
    }

    @Operation(summary = "登入（Email + 密碼）")
    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            return Result.error(401, "Invalid email or password");
        }

        UserVO user = userService.findByEmail(request.getEmail());
        if (user == null || user.getStatus() != 1) {
            return Result.error(403, "Account is inactive");
        }
        if (AuthProvider.LOCAL == user.getAuthProvider() && Boolean.FALSE.equals(user.getEmailVerified())) {
            return Result.error(403, "Email not verified. Please check your inbox.");
        }

        return Result.success(buildAuthResponse(user));
    }

    @Operation(summary = "驗證 Email（點擊信箱連結）")
    @GetMapping("/verify-email")
    public Result<String> verifyEmail(@RequestParam String token) {
        if (!userService.verifyEmailByToken(token)) {
            return Result.error(400, "Invalid or expired verification token");
        }
        return Result.success("Email verified successfully. You can now log in.");
    }

    @Operation(summary = "刷新 Access Token")
    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(@RequestParam String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken)) {
            return Result.error(401, "Invalid or expired refresh token");
        }
        UUID userId = jwtService.extractUserId(refreshToken);
        UserVO user = userService.getUserById(userId);
        if (user == null || user.getStatus() != 1) {
            return Result.error(401, "User not found or inactive");
        }
        return Result.success(buildAuthResponse(user));
    }

    private AuthResponse buildAuthResponse(UserVO user) {
        return AuthResponse.builder()
            .accessToken(jwtService.generateAccessToken(user.getUserId(), user.getEmail(), user.getRole()))
            .refreshToken(jwtService.generateRefreshToken(user.getUserId()))
            .userId(user.getUserId())
            .email(user.getEmail())
            .userName(user.getUserName())
            .role(user.getRole())
            .build();
    }
}
