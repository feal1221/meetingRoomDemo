package com.meet.meetingRoomDemo.service;

import com.meet.meetingRoomDemo.domain.room.RoomDTO;
import com.meet.meetingRoomDemo.domain.room.RoomRepository;
import com.meet.meetingRoomDemo.domain.room.RoomService;
import com.meet.meetingRoomDemo.domain.room.RoomVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomService roomService;

    private RoomVO activeRoom;
    private UUID roomId;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        activeRoom = RoomVO.builder()
            .roomId(roomId)
            .roomName("Conference A")
            .capacity(10)
            .location("3F")
            .status(1)
            .build();
    }

    // ─── getAllActiveRooms ────────────────────────────────────────────────────

    @Test
    void getAllActiveRooms_returnsOnlyStatusOne() {
        when(roomRepository.findByStatus(1)).thenReturn(List.of(activeRoom));

        List<RoomVO> result = roomService.getAllActiveRooms();

        assertEquals(1, result.size());
        assertEquals("Conference A", result.get(0).getRoomName());
        verify(roomRepository).findByStatus(1);
    }

    // ─── createRoom ──────────────────────────────────────────────────────────

    @Test
    void createRoom_savesWithStatusOne() {
        RoomDTO dto = new RoomDTO("New Room", 8, "2F");
        RoomVO saved = RoomVO.builder().roomId(roomId).roomName("New Room").capacity(8).location("2F").status(1).build();
        when(roomRepository.save(any())).thenReturn(saved);

        RoomVO result = roomService.createRoom(dto);

        assertNotNull(result);
        assertEquals("New Room", result.getRoomName());
        verify(roomRepository).save(argThat(r -> r.getStatus() == 1 && r.getCapacity() == 8));
    }

    // ─── updateRoom ──────────────────────────────────────────────────────────

    @Test
    void updateRoom_existingRoom_updatesFields() {
        RoomDTO dto = new RoomDTO("Updated Name", 20, "4F");
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(activeRoom));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RoomVO result = roomService.updateRoom(roomId, dto);

        assertEquals("Updated Name", result.getRoomName());
        assertEquals(20, result.getCapacity());
        assertEquals("4F", result.getLocation());
    }

    @Test
    void updateRoom_roomNotFound_throwsIllegalArgument() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> roomService.updateRoom(roomId, new RoomDTO("X", 1, null)));
    }

    // ─── softDeleteRoom ───────────────────────────────────────────────────────

    @Test
    void inactiveRoom_isNotReturnedByGetAllActive() {
        RoomVO inactive = RoomVO.builder().roomId(roomId).roomName("Old Room").capacity(5).status(0).build();
        when(roomRepository.findByStatus(1)).thenReturn(List.of()); // inactive not returned

        List<RoomVO> result = roomService.getAllActiveRooms();
        assertTrue(result.isEmpty());
    }

    @Test
    void softDeleteRoom_setsStatusToZero() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(activeRoom));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        roomService.softDeleteRoom(roomId);

        verify(roomRepository).save(argThat(r -> r.getStatus() == 0));
    }

    @Test
    void softDeleteRoom_roomNotFound_throwsIllegalArgument() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> roomService.softDeleteRoom(roomId));
    }

    // ─── getRoomById ──────────────────────────────────────────────────────────

    @Test
    void getRoomById_found_returnsRoom() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(activeRoom));

        RoomVO result = roomService.getRoomById(roomId);

        assertNotNull(result);
        assertEquals(roomId, result.getRoomId());
    }

    @Test
    void getRoomById_notFound_returnsNull() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        assertNull(roomService.getRoomById(roomId));
    }
}
