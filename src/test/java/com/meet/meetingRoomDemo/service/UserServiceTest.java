package com.meet.meetingRoomDemo.service;

import com.meet.meetingRoomDemo.auth.dto.RegisterRequest;
import com.meet.meetingRoomDemo.domain.user.AuthProvider;
import com.meet.meetingRoomDemo.domain.user.UserRepository;
import com.meet.meetingRoomDemo.domain.user.UserService;
import com.meet.meetingRoomDemo.domain.user.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock private UserRepository     userRepository;
    @Mock private PasswordEncoder    passwordEncoder;
    @Mock private EmailService       emailService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UserService userService;

    private UserVO userVO;

    @BeforeEach
    void setUp() {
        userVO = UserVO.builder()
            .userId(TEST_USER_ID)
            .email("test@example.com")
            .userName("Test User")
            .role(0)
            .company("Test Co")
            .pwd("password123")
            .status(1)
            .authProvider(AuthProvider.LOCAL)
            .emailVerified(false)
            .build();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ─── createUser ───────────────────────────────────────────────────────────

    @Test
    void createUser_newEmail_savesUser() {
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(null);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(userVO);

        UserVO result = userService.createUser(userVO);

        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getUserId());
        verify(userRepository).save(any(UserVO.class));
    }

    @Test
    void createUser_duplicateEmail_throwsIllegalArgument() {
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(userVO);

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(userVO));
        verify(userRepository, never()).save(any());
    }

    // ─── isEmailExists ────────────────────────────────────────────────────────

    @Test
    void isEmailExists_emailFound_returnsTrue() {
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(userVO);

        assertTrue(userService.isEmailExists("test@example.com"));
    }

    @Test
    void isEmailExists_emailNotFound_returnsFalse() {
        when(userRepository.findUserByEmail("new@example.com")).thenReturn(null);

        assertFalse(userService.isEmailExists("new@example.com"));
    }

    @Test
    void isEmailExists_nullEmail_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> userService.isEmailExists(null));
    }

    // ─── register ─────────────────────────────────────────────────────────────

    @Test
    void register_newEmail_createsUserAndSendsEmail() {
        RegisterRequest req = new RegisterRequest("New User", "new@example.com", "pass1234", null);
        UserVO saved = UserVO.builder()
            .userId(TEST_USER_ID).email("new@example.com").userName("New User")
            .role(0).status(1).authProvider(AuthProvider.LOCAL).emailVerified(false).build();

        when(userRepository.findUserByEmail("new@example.com")).thenReturn(null);
        when(passwordEncoder.encode("pass1234")).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(saved);
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

        UserVO result = userService.register(req);

        assertNotNull(result);
        assertFalse(result.getEmailVerified());
        verify(emailService).sendVerificationEmail(eq("new@example.com"), anyString());
        verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void register_duplicateEmail_throwsIllegalArgument() {
        RegisterRequest req = new RegisterRequest("User", "test@example.com", "pass1234", null);
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(userVO);

        assertThrows(IllegalArgumentException.class, () -> userService.register(req));
        verify(userRepository, never()).save(any());
    }

    // ─── verifyEmailByToken ───────────────────────────────────────────────────

    @Test
    void verifyEmailByToken_validToken_setsEmailVerifiedAndReturnsTrue() {
        String token = UUID.randomUUID().toString();
        when(valueOperations.get("verify:email:" + token)).thenReturn("test@example.com");
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(userVO);
        when(userRepository.save(any())).thenReturn(userVO);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        boolean result = userService.verifyEmailByToken(token);

        assertTrue(result);
        verify(userRepository).save(argThat(u -> Boolean.TRUE.equals(u.getEmailVerified())));
        verify(redisTemplate).delete("verify:email:" + token);
    }

    @Test
    void verifyEmailByToken_expiredToken_returnsFalse() {
        String token = UUID.randomUUID().toString();
        when(valueOperations.get("verify:email:" + token)).thenReturn(null);

        boolean result = userService.verifyEmailByToken(token);

        assertFalse(result);
        verify(userRepository, never()).save(any());
    }
}
