package com.meet.meetingRoomDemo.domain.user;

import com.meet.meetingRoomDemo.auth.dto.RegisterRequest;
import com.meet.meetingRoomDemo.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       EmailService emailService, StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public UserVO createUser(UserVO userVo) {
        String email = userVo.getEmail().toLowerCase();
        if (isEmailExists(email)) {
            throw new IllegalArgumentException("User with email " + email + " already exists");
        }
        if (userVo.getPwd() != null) {
            userVo.setPwd(passwordEncoder.encode(userVo.getPwd()));
        }
        return userRepository.save(userVo);
    }

    @Transactional
    public UserVO register(RegisterRequest request) {
        if (isEmailExists(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }
        UserVO user = UserVO.builder()
            .userName(request.getUserName())
            .email(request.getEmail().toLowerCase())
            .pwd(passwordEncoder.encode(request.getPassword()))
            .company(request.getCompany())
            .role(0)
            .status(1)
            .authProvider(AuthProvider.LOCAL)
            .emailVerified(false)
            .build();
        UserVO saved = userRepository.save(user);

        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("verify:email:" + token, saved.getEmail(), Duration.ofHours(24));
        emailService.sendVerificationEmail(saved.getEmail(), token);

        return saved;
    }

    public boolean verifyEmailByToken(String token) {
        String email = redisTemplate.opsForValue().get("verify:email:" + token);
        if (email == null) {
            return false;
        }
        UserVO user = userRepository.findUserByEmail(email.toLowerCase());
        if (user == null) {
            return false;
        }
        user.setEmailVerified(true);
        userRepository.save(user);
        redisTemplate.delete("verify:email:" + token);
        return true;
    }

    @Transactional
    public UserVO updateUser(UserVO userVo) {
        return userRepository.save(userVo);
    }

    public List<UserVO> getAllUsers() {
        return userRepository.findAll();
    }

    public UserVO getUserById(UUID userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public UserVO findByEmail(String email) {
        return userRepository.findUserByEmail(email.toLowerCase());
    }

    public boolean isEmailExists(String email) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email must not be null or empty");
        }
        return userRepository.findUserByEmail(email.toLowerCase()) != null;
    }
}
