package com.meet.meetingRoomDemo.domain.room;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public List<RoomVO> getAllActiveRooms() {
        return roomRepository.findByStatus(1);
    }

    public RoomVO getRoomById(UUID roomId) {
        return roomRepository.findById(roomId).orElse(null);
    }

    @Transactional
    public RoomVO createRoom(RoomDTO dto) {
        RoomVO room = RoomVO.builder()
            .roomName(dto.getRoomName())
            .capacity(dto.getCapacity())
            .location(dto.getLocation())
            .status(1)
            .build();
        return roomRepository.save(room);
    }

    @Transactional
    public RoomVO updateRoom(UUID roomId, RoomDTO dto) {
        RoomVO room = roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        room.setRoomName(dto.getRoomName());
        room.setCapacity(dto.getCapacity());
        room.setLocation(dto.getLocation());
        return roomRepository.save(room);
    }

    @Transactional
    public void softDeleteRoom(UUID roomId) {
        RoomVO room = roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        room.setStatus(0);
        roomRepository.save(room);
    }
}
