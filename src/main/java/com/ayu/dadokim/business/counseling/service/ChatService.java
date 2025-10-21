package com.ayu.dadokim.business.counseling.service;

import com.ayu.dadokim.business.counseling.config.OpenAIConfig;
import com.ayu.dadokim.business.counseling.domain.ChatMessage;
import com.ayu.dadokim.business.counseling.form.ChatRequestDTO;
import com.ayu.dadokim.business.counseling.repository.ChatMessageRepository;
import com.ayu.dadokim.business.user.form.UserEntity;
import com.ayu.dadokim.business.user.form.response.UserResponse;
import com.ayu.dadokim.business.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ChatService {

    private final OpenAIConfig openAIConfig;
    private final OkHttpClient client;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    public ChatService(OpenAIConfig openAIConfig,
                       ChatMessageRepository chatMessageRepository,
                       UserRepository userRepository) {
        this.openAIConfig = openAIConfig;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.client = new OkHttpClient();
    }

    /**
     * 특정 userId의 상담 내역을 기반으로 OpenAI에 요청하고,
     * user별로 대화 기록을 저장합니다.
     */
    @Transactional
    public String getChatResponse(UserResponse userResponse, ChatRequestDTO request) throws IOException {

        // ① 유저 조회 (유효성 검사)
        UserEntity user = userRepository.findByEmail(userResponse.email())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자입니다."));

        // ② 해당 유저의 이전 대화 불러오기 (null-safe)
        List<ChatMessage> history = Optional
                .ofNullable(chatMessageRepository.findByUserOrderByCreatedDateAsc(user))
                .orElseGet(ArrayList::new);

        // ③ messages JSON 구성
        JSONArray messagesArray = new JSONArray();

        // system 프롬프트 추가
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content",
                "당신은 친절하고 따뜻한 심리상담사이자 의사입니다. " +
                        "답변 시 항상 공감과 위로를 우선적으로 표현해야 합니다. " +
                        "우울증, 불안장애, 정신질환 등과 관련된 상담 질문에 성실히 답변하세요. " +
                        "또한 의사의 관점에서 정신질환 관련 약(예: 항우울제, 불안 완화제 등)에 대해서도 설명할 수 있습니다. " +
                        "단, 심리상담·정신질환과 무관한 질문에는 반드시 '저는 심리상담 및 정신질환 관련 질문에만 답변할 수 있습니다.'라고만 대답하세요. " +
                        "답변은 깔끔하게 정리해서 전달해주세요.");
        messagesArray.put(systemMessage);

        // ④ 과거 대화 기록 추가 (있을 경우에만)
        if (!history.isEmpty()) {
            history.forEach(msg -> {
                JSONObject messageJson = new JSONObject();
                messageJson.put("role", msg.getRole());
                messageJson.put("content", msg.getContent());
                messagesArray.put(messageJson);
            });
        }

        // ⑤ 현재 사용자 메시지 추가 및 DB 저장
        request.getMessages().forEach(m -> {
            JSONObject messageJson = new JSONObject();
            messageJson.put("role", m.getRole());
            messageJson.put("content", m.getContent());
            messagesArray.put(messageJson);

            // DB에 저장 (user 연관관계 포함)
            chatMessageRepository.save(ChatMessage.of(user, m.getRole(), m.getContent()));
        });

        // ⑥ OpenAI API 요청 Body 생성
        JSONObject body = new JSONObject();
        body.put("model", "gpt-4o-mini");
        body.put("messages", messagesArray);

        RequestBody requestBody = RequestBody.create(body.toString(), JSON_MEDIA_TYPE);
        Request httpRequest = new Request.Builder()
                .url(OPENAI_URL)
                .header("Authorization", "Bearer " + openAIConfig.getApiKey())
                .post(requestBody)
                .build();

        // ⑦ API 호출 및 응답 처리
        String botReply;
        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                throw new IOException("OpenAI API 호출 실패: " + response.code() + ", Body: " + errorBody);
            }

            JSONObject responseJson = new JSONObject(response.body().string());
            botReply = responseJson
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();
        }

        // ⑧ 챗봇의 답변 DB 저장 (user 연관관계 포함)
        chatMessageRepository.save(ChatMessage.of(user, "assistant", botReply));

        return botReply;
    }

    /**
     * user별 특정 기간 대화 조회 (빈 리스트 방어)
     */
    public List<ChatMessage> getChatHistoryByDateRange(UserResponse userResponse, LocalDateTime startDate, LocalDateTime endDate) {
        UserEntity user = userRepository.findByEmail(userResponse.email())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자입니다."));
        return Optional
                .ofNullable(chatMessageRepository.findByUserAndCreatedDateBetweenOrderByCreatedDateAsc(user, startDate, endDate))
                .orElseGet(ArrayList::new);
    }

    /**
     * user별 전체 대화 조회 (빈 리스트 방어)
     */
    public List<ChatMessage> getFullChatHistory(UserResponse userResponse) {
        UserEntity user = userRepository.findByEmail(userResponse.email())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자입니다."));
        return Optional
                .ofNullable(chatMessageRepository.findByUserOrderByCreatedDateAsc(user))
                .orElseGet(ArrayList::new);
    }
}
