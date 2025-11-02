package com.meet.meetingRoomDemo.service;

import com.meet.meetingRoomDemo.domain.user.UserRepository;
import com.meet.meetingRoomDemo.domain.user.UserService;
import com.meet.meetingRoomDemo.domain.user.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private UserVO userVO;

    @BeforeEach
    void setUp() {
        userVO = new UserVO();
        userVO.setUserId("1");
        userVO.setEmail("sss@gmail.com");
        userVO.setUserName("Test User");
        userVO.setRole(1);
        userVO.setCompany("Test Company");
        userVO.setPwd("password");
        userVO.setStatus(1);
        userVO.setCreatedBy("admin");
        userVO.setCreatedAt(null);
        userVO.setUpdatedBy("admin");
        userVO.setUpdatedAt(null);


    }

    @Test
    void testCreateUser() {
        when(userRepository.save(any(UserVO.class))).thenReturn(userVO);

        UserVO createdUser = userService.createUser(userVO);
        assertNotNull(createdUser);
        assertEquals("1", createdUser.getUserId());
        assertEquals("Test User", createdUser.getUserName());

        verify(userRepository, times(1)).save(userVO);
    }

    @Test
    void testIsEmailExists() {
        when(userRepository.findUserByEmail("sss@gmail.com")).thenReturn(userVO);
        Boolean exists = userService.isEmailExists("sss@gmail.com");
        assertEquals(true, exists);
        verify(userRepository, times(1)).findUserByEmail("sss@gmail.com");
    }
}