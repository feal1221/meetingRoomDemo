package com.meet.meetingRoomDemo.domain.user;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;


    @Transactional
    public UserVO createUser(UserVO userVo) {
//        TODO:確認權限
        if (isEmailExists(userVo.getEmail())) {
            throw new IllegalArgumentException("User with email " + userVo.getEmail() + " already exists");
        }
        return userRepository.save(userVo);
    }

    @Transactional
    public UserVO updateUser(UserVO userVo) {
        return userRepository.save(userVo);
    }

    @Transactional
    public List<UserVO> getAllUsers() {
//        TODO: add pagination
//        TODO:add keyword search
        return userRepository.findAll();
    }

    @Transactional
    public UserVO getUserById(String userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public Boolean isEmailExists(String email) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email must not be null or empty");
        }
        return userRepository.findUserByEmail(email.toLowerCase()) != null;
    }
}
