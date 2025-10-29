package com.ayu.dadokim.business.counseling.controller;

import com.ayu.dadokim.business.counseling.domain.ChatMessage;
import com.ayu.dadokim.business.counseling.form.ChatRequestDTO;
import com.ayu.dadokim.business.counseling.form.ChatResponseDTO;
import com.ayu.dadokim.business.counseling.service.GeminiService;
import com.ayu.dadokim.business.user.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/gemini")
public class GeminiController {

    private final GeminiService geminiService;

    public GeminiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    /**
     * 💬 Gemini 심리상담 AI와의 대화 요청 처리
     *
     * 사용자의 메시지를 받아 Gemini API로 전송하고, 모델의 응답을 반환합니다.
     * - 현재 로그인된 사용자는 JWT 인증을 통해 식별되며, `UserService.readUser()`를 통해 userId를 조회합니다.
     * - GeminiService는 이전 대화 내역을 포함하여 컨텍스트 기반 응답을 생성합니다.
     *
     * [POST] /api/gemini
     *
     * @param request 사용자의 채팅 입력 내용 (ChatRequestDTO)
     * @return Gemini 모델이 생성한 상담 응답 (ChatResponseDTO)
     * @throws IOException Gemini API 통신 중 오류 발생 시
     */
    @PostMapping
    public ChatResponseDTO chat(@RequestBody ChatRequestDTO request) throws IOException {
        return geminiService.getChatResponse(request);
    }

    /**
     * 📜 전체 상담 내역 조회
     *
     * 로그인한 사용자의 모든 상담 대화 이력을 조회합니다.
     * - 과거의 모든 질문과 응답을 시간순으로 정렬하여 반환합니다.
     *
     * [GET] /api/gemini/history/all
     *
     * @return 사용자의 전체 상담 대화 목록 (List<ChatMessage>)
     */
    @GetMapping("/history/all")
    public List<ChatMessage> getFullChatHistory() {
        return geminiService.getFullChatHistory();
    }

    /**
     * ⏰ 특정 기간의 상담 내역 조회
     *
     * 지정된 기간(`startDate` ~ `endDate`) 동안의 상담 기록만 조회합니다.
     * - 날짜는 ISO-8601 형식(예: 2025-01-01T00:00:00)으로 전달해야 합니다.
     *
     * [GET] /api/gemini/history?startDate=...&endDate=...
     *
     * @param startDate 조회 시작 시각 (LocalDateTime)
     * @param endDate   조회 종료 시각 (LocalDateTime)
     * @return 지정된 기간 내의 상담 기록 (List<ChatMessage>)
     */
    @GetMapping("/history")
    public List<ChatMessage> getChatHistoryByDateRange(
            @RequestParam("startDate") LocalDateTime startDate,
            @RequestParam("endDate") LocalDateTime endDate
    ) {
        return geminiService.getChatHistoryByDateRange(startDate, endDate);
    }
}
