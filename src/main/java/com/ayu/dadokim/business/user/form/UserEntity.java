package com.ayu.dadokim.business.user.form;

import com.ayu.dadokim.business.counseling.domain.ChatMessage;
import com.ayu.dadokim.business.user.form.Enum.SocialProviderType;
import com.ayu.dadokim.business.user.form.Enum.UserRoleType;
import com.ayu.dadokim.business.user.form.request.UserRequest;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false, updatable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "is_lock", nullable = false)
    private Boolean isLock;

    @Column(name = "is_social", nullable = false)
    private Boolean isSocial;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider_type")
    private SocialProviderType socialProviderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false)
    private UserRoleType roleType;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "email")
    private String email;

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    /** 🟢 ChatMessage와 일대다 관계 설정 **/
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> chatMessages = new ArrayList<>();

    public void updateUser(UserRequest request) {
        this.email = request.getEmail();
        this.nickname = request.getNickname();
    }

    /**
     * ✅ 헬퍼 메서드
     * ChatMessage.of()를 통해 메시지를 추가
     */
    public ChatMessage addChatMessage(String role, String content) {
        ChatMessage message = ChatMessage.of(this, role, content);
        chatMessages.add(message);
        return message;
    }
}
