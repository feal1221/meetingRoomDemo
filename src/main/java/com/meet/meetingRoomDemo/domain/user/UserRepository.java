package com.meet.meetingRoomDemo.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserVO, String>, JpaSpecificationExecutor<UserVO> {

    UserVO findUserByEmail(String email);
}
