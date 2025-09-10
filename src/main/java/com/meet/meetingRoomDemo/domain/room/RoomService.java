package com.meet.meetingRoomDemo.domain.room;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomService {
    @Autowired
    private RoomRepository roomRepository;

    @Transactional
    public RoomVO createRoom(RoomVO roomVo) {
//        TODO:check user Authority
        return roomRepository.save(roomVo);
    }

    @Transactional
    public RoomVO updateRoom(RoomVO roomVo) {
//      TODO:check user Authority
        return roomRepository.save(roomVo);
    }

    @Transactional
    public RoomVO getRoomById(String roomId) {
        return roomRepository.findById(roomId).orElse(null);
    }

    public void deleteRoomById(String roomId) {
//        TODO: check if room is in use
        roomRepository.deleteById(roomId);
    }

    public List<RoomVO> getAllRooms() {
        return roomRepository.findAll();
    }


}
