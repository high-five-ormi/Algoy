package com.example.algoyweb.service.chatting;

import com.example.algoyweb.exception.CustomException;
import com.example.algoyweb.exception.chatting.ChattingErrorCode;
import com.example.algoyweb.model.dto.chatting.ChattingDto;
import com.example.algoyweb.model.dto.chatting.ChattingRoomDto;
import com.example.algoyweb.model.entity.chatting.Chatting;
import com.example.algoyweb.model.entity.chatting.ChattingRoom;
import com.example.algoyweb.model.entity.user.User;
import com.example.algoyweb.repository.chatting.ChattingRepository;
import com.example.algoyweb.repository.chatting.ChattingRoomRepository;
import com.example.algoyweb.repository.user.UserRepository;
import com.example.algoyweb.util.chatting.ChattingConvertUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@EnableAsync
@Transactional
@RequiredArgsConstructor
public class ChattingService {

  private final ChattingRepository chattingRepository;
  private final ChattingRoomRepository chattingRoomRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public Page<ChattingRoomDto> getAllRooms(Pageable pageable) {
    Page<ChattingRoom> rooms = chattingRoomRepository.findAll(pageable);
    return rooms.map(ChattingConvertUtil::convertToDto);
  }

  @Transactional(readOnly = true)
  public Page<ChattingRoomDto> getRoomsForUser(String username, Pageable pageable) {
    User user = getUserByUsername(username);
    Page<ChattingRoom> rooms = chattingRoomRepository.findByParticipantsContaining(user.getUserId(), pageable);
    return rooms.map(ChattingConvertUtil::convertToDto);
  }

  @Transactional(readOnly = true)
  public Page<ChattingDto> getRoomMessages(String roomId, Pageable pageable) {
    Page<Chatting> messages = chattingRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);
    return messages.map(ChattingConvertUtil::convertToDto);
  }

  public ChattingRoomDto createRoom(String roomName, String username) {
    User owner = getUserByUsername(username);

    ChattingRoom chattingRoom = ChattingRoom.builder()
        .roomId("room-" + System.currentTimeMillis())
        .name(roomName)
        .owner(owner)
        .participants(new ArrayList<>(List.of(owner.getUserId())))
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    ChattingRoom savedRoom = chattingRoomRepository.save(chattingRoom);
    return ChattingConvertUtil.convertToDto(savedRoom);
  }

  public void joinRoom(String roomId, String username) {
    User user = getUserByUsername(username);
    ChattingRoom room = getChattingRoomByRoomId(roomId);
    if (!room.getParticipants().contains(user.getUserId())) {
      room.addParticipant(user.getUserId());
      chattingRoomRepository.save(room);
    }
  }

  public void leaveRoom(String roomId, String username) {
    User user = getUserByUsername(username);
    ChattingRoom room = getChattingRoomByRoomId(roomId);
    room.removeParticipant(user.getUserId());
    chattingRoomRepository.save(room);
  }

  public void inviteToRoom(String roomId, String inviterUsername, Long inviteeId) {
    User inviter = getUserByUsername(inviterUsername);
    ChattingRoom room = getChattingRoomByRoomId(roomId);

    if (!room.getOwner().getUserId().equals(inviter.getUserId())) {
      throw new CustomException(ChattingErrorCode.NOT_ROOM_OWNER);
    }

    User invitee = userRepository.findById(inviteeId)
        .orElseThrow(() -> new CustomException(ChattingErrorCode.USER_NOT_FOUND));

    room.addParticipant(inviteeId);
    chattingRoomRepository.save(room);
  }

  @Async
  public void saveMessage(ChattingDto chattingDto) {
    User user = getUserById(chattingDto.getUserId());
    ChattingRoom room = getChattingRoomByRoomId(chattingDto.getRoomId());

    if (!room.getParticipants().contains(user.getUserId())) {
      throw new CustomException(ChattingErrorCode.USER_NOT_IN_ROOM);
    }

    Chatting chatting = ChattingConvertUtil.convertToEntity(chattingDto, user);
    chattingRepository.save(chatting);
  }

  private User getUserByUsername(String username) {
    return userRepository.findByEmail(username);
  }

  private User getUserById(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(ChattingErrorCode.USER_NOT_FOUND));
  }

  private ChattingRoom getChattingRoomByRoomId(String roomId) {
    return chattingRoomRepository.findByRoomId(roomId)
        .orElseThrow(() -> new CustomException(ChattingErrorCode.ROOM_NOT_FOUND));
  }

  public String joinRoom(String roomId, Long userId) {
    User user = getUserById(userId);
    ChattingRoom room = getChattingRoomByRoomId(roomId);
    if (!room.getParticipants().contains(user.getUserId())) {
      room.addParticipant(user.getUserId());
      chattingRoomRepository.save(room);
    }
    return user.getNickname();
  }

  public String leaveRoom(String roomId, Long userId) {
    User user = getUserById(userId);
    ChattingRoom room = getChattingRoomByRoomId(roomId);
    room.removeParticipant(user.getUserId());
    chattingRoomRepository.save(room);
    return user.getNickname();
  }
}