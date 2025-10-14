package com.ayu.dadokim.business.counseling.service;

import com.ayu.dadokim.business.user.form.response.UserResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;
import com.ayu.dadokim.business.counseling.config.GeminiConfig;
import com.ayu.dadokim.business.counseling.domain.ChatMessage;
import com.ayu.dadokim.business.counseling.form.ChatRequestDTO;
import com.ayu.dadokim.business.counseling.form.ChatResponseDTO;
import com.ayu.dadokim.business.counseling.repository.ChatMessageRepository;
import com.ayu.dadokim.business.user.form.UserEntity;
import com.ayu.dadokim.business.user.repository.UserRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GeminiService {

    private final OkHttpClient client;
    private final GeminiConfig geminiConfig;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    public GeminiService(GeminiConfig geminiConfig,
                         ChatMessageRepository chatMessageRepository,
                         UserRepository userRepository) {
        this.client = new OkHttpClient();
        this.geminiConfig = geminiConfig;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    /**
     * 사용자별 메시지를 처리하고 Gemini API에 요청하여 답변을 받습니다.
     * 모든 대화 기록을 불러와 컨텍스트를 유지하고,
     * 새로운 메시지와 답변을 DB에 저장합니다.
     *
     * @param request 사용자 메시지를 포함하는 DTO
     * @return Gemini 모델의 답변
     * @throws IOException API 통신 오류 발생 시
     */
    public ChatResponseDTO getChatResponse(UserResponse userResponse, ChatRequestDTO request) throws IOException {

        // ① userId 기반 사용자 정보 조회
        UserEntity user = userRepository.findByEmail(userResponse.email())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자입니다."));

        JSONArray contentsArray = new JSONArray();

        // ② 사용자별 기존 대화 내역 조회 (빈 리스트 방어)
        List<ChatMessage> history = Optional
                .ofNullable(chatMessageRepository.findByUserOrderByCreatedDateAsc(user))
                .orElseGet(ArrayList::new);

        // 🧠 이전 대화가 존재할 경우만 Gemini 요청 본문에 포함
        if (!history.isEmpty()) {
            history.forEach(msg -> {
                JSONObject contentJson = new JSONObject();
                contentJson.put("role", msg.getRole());
                JSONArray parts = new JSONArray().put(new JSONObject().put("text", msg.getContent()));
                contentJson.put("parts", parts);
                contentsArray.put(contentJson);
            });
        }

        // ③ 시스템 프롬프트 추가
        JSONObject systemContent = new JSONObject();
        JSONArray systemParts = new JSONArray().put(
                new JSONObject().put("text",
                        "당신은 친절하고 따뜻한 심리상담사이자 의사입니다. " +
                                "답변 시 항상 공감과 위로를 우선적으로 표현해야 합니다. " +
                                "우울증, 불안장애, 정신질환 등과 관련된 상담 질문에 성실히 답변하세요. " +
                                "또한 의사의 관점에서 정신질환과 관련된 약(예: 항우울제, 불안 완화제 등)에 대해서도 설명하고 조언할 수 있습니다. " +
                                "단, 심리상담·정신질환·정신질환 약과 무관한 질문이 들어오면 반드시 " +
                                "'저는 심리상담 및 정신질환 관련 질문에만 답변할 수 있습니다.' 라고만 대답하세요. " +
                                "답변은 최대한 깔끔하게 정리해서 한 문단이 끝나면 '\\n'을 사용해서 한 줄 띄어서 답변하세요.")
        );
        systemContent.put("role", "user");
        systemContent.put("parts", systemParts);
        contentsArray.put(systemContent);

        // ④ 현재 사용자의 새로운 메시지를 API 요청에 포함시키고 DB에 저장
        request.getMessages().forEach(msg -> {
            JSONObject contentJson = new JSONObject();
            contentJson.put("role", msg.getRole());
            JSONArray parts = new JSONArray().put(new JSONObject().put("text", msg.getContent()));
            contentJson.put("parts", parts);
            contentsArray.put(contentJson);

            // DB 저장 (user 연관관계 포함)
            ChatMessage userMessage = ChatMessage.builder()
                    .role(msg.getRole())
                    .content(msg.getContent())
                    .createdDate(LocalDateTime.now())
                    .user(user)
                    .build();
            chatMessageRepository.save(userMessage);
        });

        // ⑤ Gemini API 요청 본문 생성
        JSONObject body = new JSONObject();
        body.put("contents", contentsArray);

        RequestBody requestBody = RequestBody.create(
                body.toString(),
                MediaType.parse("application/json")
        );

        // ⑥ Gemini API 호출
        Request httpRequest = new Request.Builder()
                .url(GEMINI_URL + "?key=" + geminiConfig.getApiKey())
                .post(requestBody)
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Gemini API 호출 실패: " + response);
            }

            // ⑦ Gemini API 응답 파싱
            JSONObject responseJson = new JSONObject(response.body().string());
            String reply = responseJson
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim();

            // ⑧ Gemini 답변을 DB에 저장 (user 연관관계 포함)
            ChatMessage assistantMessage = ChatMessage.builder()
                    .role("assistant")
                    .content(reply)
                    .createdDate(LocalDateTime.now())
                    .user(user)
                    .build();
            chatMessageRepository.save(assistantMessage);

            // ⑨ 답변 포맷 정리 후 반환
            String[] sentences = reply.split("(?<=[.?!])\\s+");
            StringBuilder formattedReply = new StringBuilder();
            for (String sentence : sentences) {
                formattedReply.append(sentence);
            }

            String finalReply = formattedReply.toString().trim();
            return new ChatResponseDTO("assistant", finalReply);
        }
    }

    /**
     * 특정 사용자(userId)의 지정된 기간 동안의 상담 기록 조회
     */
    public List<ChatMessage> getChatHistoryByDateRange(UserResponse userResponse, LocalDateTime startDate, LocalDateTime endDate) {
        UserEntity user = userRepository.findByEmail(userResponse.email())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자입니다."));
        return chatMessageRepository.findByUserAndCreatedDateBetweenOrderByCreatedDateAsc(user, startDate, endDate);
    }

    /**
     * 특정 사용자(userId)의 전체 상담 대화 기록 조회
     */
    public List<ChatMessage> getFullChatHistory(UserResponse userResponse) {
        UserEntity user = userRepository.findByEmail(userResponse.email())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자입니다."));
        return Optional
                .ofNullable(chatMessageRepository.findByUserOrderByCreatedDateAsc(user))
                .orElseGet(ArrayList::new); // 빈 리스트 방어
    }
}
