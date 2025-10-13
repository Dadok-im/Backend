package com.ayu.dadokim.business.counseling.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ChatMessage {
    @Id
    @GeneratedValue
            (strategy = GenerationType.IDENTITY)
    private Long id;

    private String role;     // "user" or "assistant"
    @Lob
    @Column(columnDefinition = "LONGTEXT") // LONGTEXT로 명시적으로 지정
    private String content;
    @Column(nullable = false)
    private LocalDateTime createdDate; // 메시지 생성 시간

}
