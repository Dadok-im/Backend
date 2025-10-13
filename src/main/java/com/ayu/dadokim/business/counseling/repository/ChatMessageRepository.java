package com.ayu.dadokim.business.counseling.repository;

import com.ayu.dadokim.business.counseling.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 특정 역할("user" 또는 "assistant")의 채팅 기록을 조회하는 메서드
    List<ChatMessage> findByRole(String role);

    // 시작 날짜와 끝 날짜 사이의 채팅 기록을 조회하는 메서드
    List<ChatMessage> findByCreatedDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    // 모든 채팅 기록을 오래된 순서(오름차순)로 정렬하여 조회하는 메서드
    List<ChatMessage> findAllByOrderByCreatedDateAsc();
}