package com.ayu.dadokim.business.counseling.domain;

import com.ayu.dadokim.business.user.form.UserEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String role; // "user" or "assistant"

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdDate;

    /**
     * ✅ UserEntity와 다대일 관계 설정
     *    - fetch: LAZY (필요할 때만 조회)
     *    - 외래키 이름 명시 (FK_CHAT_MESSAGE_USER)
     */
    @JsonIgnoreProperties({"chatMessages"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",                      // 실제 컬럼명
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_CHAT_MESSAGE_USER") // FK 이름 명시
    )
    private UserEntity user;

    /**
     * ✅ 정적 팩토리 메서드
     * ChatMessage를 생성할 때 반드시 User를 지정하도록 강제
     */
    public static ChatMessage of(UserEntity user, String role, String content) {
        return ChatMessage.builder()
                .user(user)
                .role(role)
                .content(content)
                .createdDate(LocalDateTime.now())
                .build();
    }
}
