package com.ayu.dadokim.business.counseling.service;

import com.ayu.dadokim.business.counseling.domain.ChatMessage;
import com.ayu.dadokim.business.counseling.form.ChatRequestDTO;
import com.ayu.dadokim.business.counseling.config.OpenAIConfig;
import com.ayu.dadokim.business.counseling.repository.ChatMessageRepository;

import jakarta.transaction.Transactional;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final OpenAIConfig openAIConfig;
    private final OkHttpClient client;
    private final ChatMessageRepository chatMessageRepository;
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    public ChatService(OpenAIConfig openAIConfig, ChatMessageRepository chatMessageRepository) {
        this.openAIConfig = openAIConfig;
        this.client = new OkHttpClient();
        this.chatMessageRepository = chatMessageRepository;
    }

    /**
     * 사용자 메시지를 처리하고 OpenAI API에 요청하여 답변을 받습니다.
     * 모든 대화 기록을 불러와 컨텍스트를 유지하고, 새로운 메시지와 답변을 DB에 저장합니다.
     *
     * @param request 사용자 메시지를 포함하는 DTO
     * @return 챗봇의 답변 문자열
     * @throws IOException API 통신 오류 발생 시
     */
    @Transactional
    public String getChatResponse(ChatRequestDTO request) throws IOException {

        // 1. 이전 대화 기록을 DB에서 불러와 컨텍스트로 사용
        // 현재는 userId 없이 모든 메시지를 불러옴 (다음 단계에서 userId 기반으로 수정 필요)
        List<ChatMessage> history = chatMessageRepository.findAllByOrderByCreatedDateAsc();

        // 2. 메시지를 JSON 배열로 변환 (System Prompt + History + User Message)
        JSONArray messagesArray = new JSONArray();

        // System Prompt 추가
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content",
                "당신은 친절하고 따뜻한 심리상담사이자 의사입니다. " +
                        "답변 시 항상 공감과 위로를 우선적으로 표현해야 합니다. " +
                        "우울증, 불안장애, 정신질환 등과 관련된 상담 질문에 성실히 답변하세요. " +
                        "또한 의사의 관점에서 정신질환과 관련된 약(예: 항우울제, 불안 완화제 등)에 대해서도 설명하고 조언할 수 있습니다. " +
                        "단, 심리상담·정신질환·정신질환 약과 무관한 질문이 들어오면 반드시 " +
                        "'저는 심리상담 및 정신질환 관련 질문에만 답변할 수 있습니다.' 라고만 대답하세요."+
                        "답변은 최대한 깔끔하게 정리해서 답변해주세요."
        );
        messagesArray.put(systemMessage);

        // 이전 대화 기록 추가
        history.forEach(msg -> {
            JSONObject messageJson = new JSONObject();
            messageJson.put("role", msg.getRole());
            messageJson.put("content", msg.getContent());
            messagesArray.put(messageJson);
        });

        // 현재 사용자 메시지 추가 (request DTO에는 현재 메시지만 포함됨)
        // DTO의 List<Message>에서 마지막 메시지만 처리한다고 가정 (혹은 전체 리스트를 사용)
        // 여기서는 DTO가 하나의 메시지(현재 질문)만 담고 있다고 가정하고 처리합니다.
        request.getMessages().forEach(msg -> {
            JSONObject messageJson = new JSONObject();
            messageJson.put("role", msg.getRole());
            messageJson.put("content", msg.getContent());
            messagesArray.put(messageJson);

            // 3. 사용자 메시지 DB에 저장
            ChatMessage userMessage = ChatMessage.builder()
                    .role(msg.getRole())
                    .content(msg.getContent())
                    .createdDate(LocalDateTime.now())
                    .build();
            chatMessageRepository.save(userMessage);
        });

        // 4. 요청 body 생성
        JSONObject body = new JSONObject();
        body.put("model", "gpt-4o-mini"); // API 모델 설정
        body.put("messages", messagesArray);

        RequestBody requestBody = RequestBody.create(body.toString(), JSON_MEDIA_TYPE);

        // 5. OpenAI API 호출
        Request httpRequest = new Request.Builder()
                .url(OPENAI_URL)
                .header("Authorization", "Bearer " + openAIConfig.getApiKey())
                .post(requestBody)
                .build();

        String botReply;
        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                // API 호출 실패 시 예외 처리
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                throw new IOException("OpenAI API 호출 실패: " + response.code() + ", Body: " + errorBody);
            }

            // 6. 응답 파싱
            JSONObject responseJson = new JSONObject(response.body().string());
            botReply = responseJson
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();
        }

        // 7. 챗봇 답변 DB에 저장
        ChatMessage assistantMessage = ChatMessage.builder()
                .role("assistant")
                .content(botReply)
                .createdDate(LocalDateTime.now())
                .build();
        chatMessageRepository.save(assistantMessage);

        return botReply;
    }

    /**
     * 특정 날짜 범위의 채팅 기록을 조회합니다.
     *
     * @param startDate 조회 시작 날짜 및 시간
     * @param endDate   조회 종료 날짜 및 시간
     * @return 주어진 기간에 해당하는 ChatMessage 리스트
     */
    public List<ChatMessage> getChatHistoryByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return chatMessageRepository.findByCreatedDateBetween(startDate, endDate);
    }

    /**
     * 모든 채팅 기록을 오래된 순서로 조회합니다.
     *
     * @return 모든 ChatMessage 리스트 (오래된 순)
     */
    public List<ChatMessage> getFullChatHistory() {
        return chatMessageRepository.findAllByOrderByCreatedDateAsc();
    }
}
