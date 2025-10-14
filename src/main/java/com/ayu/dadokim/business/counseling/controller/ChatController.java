package com.ayu.dadokim.business.counseling.controller;

import com.ayu.dadokim.business.counseling.domain.ChatMessage;
import com.ayu.dadokim.business.counseling.form.ChatRequestDTO;
import com.ayu.dadokim.business.counseling.form.ChatResponseDTO;
import com.ayu.dadokim.business.counseling.service.ChatService;
import com.ayu.dadokim.business.user.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 📘 ChatController
 * ----------------------------
 * 사용자의 상담 요청 및 상담 기록 조회를 처리하는 REST API 컨트롤러입니다.
 *
 * 주요 기능:
 * 1. OpenAI 기반 상담 응답 생성
 * 2. 사용자별 전체 상담 이력 조회
 * 3. 특정 기간 동안의 상담 내역 조회
 *
 * 모든 요청은 인증된 사용자(JWT 기반)만 접근 가능합니다.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    public ChatController(ChatService chatService, UserService userService) {
        this.chatService = chatService;
        this.userService = userService;
    }

    /**
     * 💬 새 상담 메시지 전송 및 AI 응답 반환
     * ----------------------------------
     * 사용자가 보낸 메시지를 OpenAI API로 전달하여 AI의 응답을 생성합니다.
     *
     * ✅ 요청
     * - URL: {@code POST /api/chat}
     * - Body: {@link ChatRequestDTO} (사용자 메시지, 대화 컨텍스트 포함)
     *
     * ✅ 응답
     * - {@link ChatResponseDTO} (AI 응답 메시지)
     *
     * ✅ 인증
     * - JWT 기반 사용자 인증 필요 (userId 자동 추출)
     *
     * @param request 사용자가 보낸 채팅 요청 데이터
     * @return AI의 상담 응답 메시지
     * @throws IOException OpenAI API 호출 중 오류 발생 시
     */
    @PostMapping
    public ChatResponseDTO chat(@RequestBody ChatRequestDTO request) throws IOException {
        String reply = chatService.getChatResponse(userService.readUser(), request);
        return new ChatResponseDTO("assistant", reply);
    }

    /**
     * 📜 전체 상담 내역 조회
     * ----------------------------
     * 인증된 사용자의 모든 상담 기록을 반환합니다.
     *
     * ✅ 요청
     * - URL: {@code GET /api/chat/history/all}
     *
     * ✅ 응답
     * - {@code List<ChatMessage>} (전체 상담 내역)
     *
     * @return 사용자별 전체 상담 내역 리스트
     */
    @GetMapping("/history/all")
    public List<ChatMessage> getFullChatHistory() {
        return chatService.getFullChatHistory(userService.readUser());
    }

    /**
     * ⏰ 기간별 상담 내역 조회
     * ----------------------------
     * 지정한 기간(startDate ~ endDate) 동안의 상담 내역을 조회합니다.
     *
     * ✅ 요청
     * - URL: {@code GET /api/chat/history?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59}
     * - Query Params:
     *   - {@code startDate} (조회 시작 시간)
     *   - {@code endDate} (조회 종료 시간)
     *
     * ✅ 응답
     * - {@code List<ChatMessage>} (기간 내 상담 내역)
     *
     * @param startDate 조회 시작일시 (ISO-8601 형식)
     * @param endDate   조회 종료일시 (ISO-8601 형식)
     * @return 지정된 기간의 상담 내역 리스트
     */
    @GetMapping("/history")
    public List<ChatMessage> getChatHistoryByDateRange(
            @RequestParam("startDate") LocalDateTime startDate,
            @RequestParam("endDate") LocalDateTime endDate
    ) {
        return chatService.getChatHistoryByDateRange(userService.readUser(), startDate, endDate);
    }
}
