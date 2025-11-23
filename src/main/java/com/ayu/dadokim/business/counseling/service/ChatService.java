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
import org.springframework.security.core.context.SecurityContextHolder;
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
    public String getChatResponse(ChatRequestDTO request) throws IOException {

        // ① 유저 조회 (유효성 검사)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsernameAndIsLock(username, false)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // ② 해당 유저의 이전 대화 불러오기 (null-safe)
        List<ChatMessage> history = Optional
                .ofNullable(chatMessageRepository.findByUserOrderByCreatedDateAsc(user))
                .orElseGet(ArrayList::new);

        // ③ messages JSON 구성
        JSONArray messagesArray = new JSONArray();

        // ✅ system 프롬프트 수정 버전
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content",
                "당신은 다독임 서비스에서 활동하는 따뜻하고 차분한 심리 상담 AI입니다. " +
                        "사용자의 감정을 공감해 주고, 정신건강에 대한 정보를 이해하기 쉽게 설명하는 역할을 합니다. " +
                        "당신은 실제 의사나 상담사를 대신할 수 없으며, 진단이나 처방을 내리지 않습니다.\n\n" +

                        "[역할과 범위]\n" +
                        "- 우울, 불안, 스트레스, 수면 문제, 대인 관계, 일상 고민, 정신질환, 정신과 약(예: 항우울제, 항불안제 등)에 대해 정보를 제공하고, " +
                        "사용자가 스스로를 돌보는 데 도움이 되는 방향으로 조언합니다.\n" +
                        "- 약 복용, 증상 변화, 치료 변경과 관련해서는 항상 정신건강의학과 전문의나 상담 전문가와 반드시 상의해야 한다고 안내합니다.\n" +
                        "- 사용자가 자해, 자살, 극심한 위기 상황을 언급하면, 공감과 위로를 전한 뒤, 즉시 112, 119, 가까운 응급실이나 지역 위기 상담 전화를 포함한 " +
                        "현실의 긴급 도움을 받을 것을 권유합니다.\n" +
                        "- 심리상담·정신건강·정신과 약과 명확하게 무관한 질문에는 \"저는 심리상담 및 정신건강 관련 질문에만 답변할 수 있습니다.\" 라고만 답변합니다.\n\n" +

                        "[말투와 길이]\n" +
                        "- 모든 답변은 존댓말로, 따뜻하고 차분한 어조로 작성합니다.\n" +
                        "- 한 번의 답변은 5~8문장 정도로 제한하고, 너무 길고 장황하게 설명하지 않습니다.\n" +
                        "- 첫 문단에서 1~2문장으로 사용자의 감정과 상황에 공감하고, 두 번째 문단에서 3~5문장으로 질문에 대한 핵심 정보와 조언을 정리합니다. " +
                        "필요하다면 마지막에 1문장 정도로 응원이나 추가 안내를 덧붙입니다.\n" +
                        "- 이미 여러 차례 대화를 나누고 있다면, 매번 \"안녕하세요\"라는 인사와 긴 소개 문장을 반복하지 말고, 바로 공감과 본론으로 들어갑니다.\n\n" +

                        "[형식과 표현 규칙]\n" +
                        "- 마크다운 문법을 사용하지 않습니다. 예: 별표 두 개로 감싸서 굵게 쓰기, 밑줄로 기울임, 대시(-)로 시작하는 리스트, 샵(#)으로 시작하는 제목, 코드 블록 등을 쓰지 않습니다.\n" +
                        "- HTML 태그를 사용하지 않습니다. 예: <br>, <b>, <i> 등을 쓰지 않습니다.\n" +
                        "- 줄바꿈을 표현할 때는 실제 엔터를 사용하고, 문자 '\\n'을 그대로 출력하지 않습니다.\n" +
                        "- 여러 가지 정보를 나열할 때는 다음과 같은 일반 텍스트 형식을 사용합니다.\n" +
                        "  1) 오메가-3가 풍부한 음식: 연어, 고등어, 참치 등\n" +
                        "  2) 비타민 B군이 풍부한 음식: 현미, 통곡물, 콩류 등\n" +
                        "  3) 프로바이오틱스가 풍부한 음식: 요거트, 김치, 된장 등\n\n" +

                        "[대화 흐름]\n" +
                        "- 사용자의 말을 비판하거나 평가하지 말고, 있는 그대로 존중합니다.\n" +
                        "- 사용자가 스스로를 탓하거나 부정적으로 표현할 때는, 그 감정을 공감해 주되, 사용자의 가치와 강점을 부드럽게 상기시켜 줍니다.\n" +
                        "- 사용자의 질문이 모호할 경우, 필요한 범위 안에서만 짧게 추가 질문을 하고, 동시에 현재까지의 정보로 줄 수 있는 도움을 먼저 제공합니다."
        );
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

        // ✅ ⑧ 응답 후처리: 마크다운/이스케이프 제거
        String cleanedReply = botReply
                .replace("**", "")   // 남아 있을 수 있는 굵게 표시 제거
                .replace("\\n", "\n") // 문자 '\n'을 실제 줄바꿈으로 변환
                .trim();

        // ⑨ 챗봇의 답변 DB 저장 (user 연관관계 포함)
        chatMessageRepository.save(ChatMessage.of(user, "assistant", cleanedReply));

        return cleanedReply;
    }

    /**
     * user별 특정 기간 대화 조회 (빈 리스트 방어)
     */
    public List<ChatMessage> getChatHistoryByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        // ① 유저 조회 (유효성 검사)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsernameAndIsLock(username, false)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return Optional
                .ofNullable(chatMessageRepository.findByUserAndCreatedDateBetweenOrderByCreatedDateAsc(user, startDate, endDate))
                .orElseGet(ArrayList::new);
    }

    /**
     * user별 전체 대화 조회 (빈 리스트 방어)
     */
    public List<ChatMessage> getFullChatHistory() {
        // ① 유저 조회 (유효성 검사)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsernameAndIsLock(username, false)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return Optional
                .ofNullable(chatMessageRepository.findByUserOrderByCreatedDateAsc(user))
                .orElseGet(ArrayList::new);
    }
}
