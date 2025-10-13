package com.ayu.dadokim.business.counseling.service;

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
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class GeminiService {

    private final OkHttpClient client;
    private final GeminiConfig geminiConfig;
    private final ChatMessageRepository chatMessageRepository;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    public GeminiService(GeminiConfig geminiConfig, ChatMessageRepository chatMessageRepository) {
        this.client = new OkHttpClient();
        this.geminiConfig = geminiConfig;
        this.chatMessageRepository = chatMessageRepository;
    }

    public ChatResponseDTO getChatResponse(ChatRequestDTO request) throws IOException {
        JSONArray contentsArray = new JSONArray();

        // 1. 모든 이전 대화 기록을 데이터베이스에서 불러와 API 요청에 포함시킵니다.
        List<ChatMessage> history = chatMessageRepository.findAllByOrderByCreatedDateAsc();
        history.forEach(msg -> {
            JSONObject contentJson = new JSONObject();
            contentJson.put("role", msg.getRole());
            JSONArray parts = new JSONArray().put(new JSONObject().put("text", msg.getContent()));
            contentJson.put("parts", parts);
            contentsArray.put(contentJson);
        });

        // 시스템 프롬프트는 API 요청 본문에 직접 포함하는 것보다,
        // Gemini API의 `systemInstruction` 필드를 사용하는 것이 더 권장됩니다.
        // 현재 코드에서는 메시지 배열에 첫 번째로 추가합니다.
        JSONObject systemContent = new JSONObject();
        JSONArray systemParts = new JSONArray().put(
                new JSONObject().put("text", "당신은 친절하고 따뜻한 심리상담사이자 의사입니다. " +
                        "답변 시 항상 공감과 위로를 우선적으로 표현해야 합니다. " +
                        "우울증, 불안장애, 정신질환 등과 관련된 상담 질문에 성실히 답변하세요. " +
                        "또한 의사의 관점에서 정신질환과 관련된 약(예: 항우울제, 불안 완화제 등)에 대해서도 설명하고 조언할 수 있습니다. " +
                        "단, 심리상담·정신질환·정신질환 약과 무관한 질문이 들어오면 반드시 " +
                        "'저는 심리상담 및 정신질환 관련 질문에만 답변할 수 있습니다.' 라고만 대답하세요." +
                        "답변은 최대한 깔끔하게 정리해서 한 문단이 끝나면 '\n'을 사용해서 한 줄을 띄어서 답변하세요.")
        );
        systemContent.put("role", "user");
        systemContent.put("parts", systemParts);
        contentsArray.put(systemContent);

        // 2. 현재 사용자 메시지를 API 요청에 추가합니다.
        request.getMessages().forEach(msg -> {
            JSONObject contentJson = new JSONObject();
            contentJson.put("role", msg.getRole());
            JSONArray parts = new JSONArray().put(new JSONObject().put("text", msg.getContent()));
            contentJson.put("parts", parts);
            contentsArray.put(contentJson);

            // 3. 사용자 메시지를 데이터베이스에 저장합니다.
            ChatMessage userMessage = ChatMessage.builder()
                    .role(msg.getRole())
                    .content(msg.getContent())
                    .createdDate(LocalDateTime.now())
                    .build();
            chatMessageRepository.save(userMessage);
        });

        JSONObject body = new JSONObject();
        body.put("contents", contentsArray);

        RequestBody requestBody = RequestBody.create(
                body.toString(),
                MediaType.parse("application/json")
        );

        Request httpRequest = new Request.Builder()
                .url(GEMINI_URL + "?key=" + geminiConfig.getApiKey())
                .post(requestBody)
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Gemini API 호출 실패: " + response);
            }

            JSONObject responseJson = new JSONObject(response.body().string());

            String reply = responseJson
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim();

            // 4. API 답변을 데이터베이스에 저장합니다.
            ChatMessage assistantMessage = ChatMessage.builder()
                    .role("assistant")
                    .content(reply)
                    .createdDate(LocalDateTime.now())
                    .build();
            chatMessageRepository.save(assistantMessage);

            // 답변 형식을 깔끔하게 정리하는 기존 로직
            String[] sentences = reply.split("(?<=[.?!])\\s+");
            StringBuilder formattedReply = new StringBuilder();
            for (String sentence : sentences) {
                formattedReply.append(sentence).append("");
            }

            String finalReply = formattedReply.toString().trim();
            return new ChatResponseDTO("assistant", finalReply);
        }
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
     * 이 메서드는 초기 화면에서 이전 대화 내용을 불러오는 데 사용될 수 있습니다.
     *
     * @return 모든 ChatMessage 리스트 (오래된 순)
     */
    public List<ChatMessage> getFullChatHistory() {
        return chatMessageRepository.findAllByOrderByCreatedDateAsc();
    }
}
