package com.ayu.dadokim.business.counseling.controller;

import com.ayu.dadokim.business.counseling.domain.ChatMessage;
import com.ayu.dadokim.business.counseling.form.ChatRequestDTO;
import com.ayu.dadokim.business.counseling.form.ChatResponseDTO;
import com.ayu.dadokim.business.counseling.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:5173") // React 개발 서버 허용
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 사용자의 새로운 채팅 메시지를 처리하고 OpenAI API 응답을 반환합니다.
     * HTTP POST 요청을 통해 ChatRequestDTO를 받아 서비스 계층으로 전달합니다.
     * URL: /api/chat
     */
    @PostMapping
    public ChatResponseDTO chat(@RequestBody ChatRequestDTO request) throws IOException {
        String reply = chatService.getChatResponse(request);
        return new ChatResponseDTO("assistant", reply); // role을 함께 전달
    }

    /**
     * 모든 채팅 기록을 조회합니다.
     * 주로 초기 화면에서 이전 대화 내용을 불러오는 데 사용됩니다.
     * URL: /api/chat/history/all
     */
    @GetMapping("/history/all")
    public List<ChatMessage> getFullChatHistory() {
        return chatService.getFullChatHistory();
    }

    /**
     * 특정 기간 동안의 채팅 기록을 조회합니다.
     * URL: /api/chat/history
     * 예시: /api/chat/history?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59
     */
    @GetMapping("/history")
    public List<ChatMessage> getChatHistoryByDateRange(
            @RequestParam("startDate") LocalDateTime startDate,
            @RequestParam("endDate") LocalDateTime endDate) {
        return chatService.getChatHistoryByDateRange(startDate, endDate);
    }
}
