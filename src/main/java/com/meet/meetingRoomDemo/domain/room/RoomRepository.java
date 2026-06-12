package com.meet.meetingRoomDemo.domain.room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<RoomVO, UUID>, JpaSpecificationExecutor<RoomVO> {

    List<RoomVO> findByStatus(Integer status);
}
