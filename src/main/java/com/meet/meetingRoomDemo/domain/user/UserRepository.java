package com.meet.meetingRoomDemo.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserVO, UUID>, JpaSpecificationExecutor<UserVO> {

    UserVO findUserByEmail(String email);
}
