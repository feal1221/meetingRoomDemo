package com.meet.meetingRoomDemo.domain.record;


import com.meet.meetingRoomDemo.domain.room.RoomVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecordRepository extends JpaRepository<RecordDTO, String>, JpaSpecificationExecutor<RecordDTO> {
}
