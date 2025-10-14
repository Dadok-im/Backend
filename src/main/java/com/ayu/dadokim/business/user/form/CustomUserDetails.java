package com.ayu.dadokim.business.user.form;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    private final Long UserId;
    private final String role;  // 소셜 로그인 ID

    public CustomUserDetails(UserEntity user) {
        this.UserId = user.getId();
        this.role = "ROLE_" + user.getRoleType();
    }

    public CustomUserDetails(Long UserId, String role) {
        this.UserId = UserId;
        this.role = role;
    }

    public Long getMemberId() {
        return this.UserId;
    }

    @Override
    public String getPassword() {
        return null; // 소셜 로그인에서는 비밀번호 없음
    }

    @Override
    public String getUsername() {
        // UserDetails의 username은 고유 식별자 역할을 하므로 memberId를 반환
        return String.valueOf(this.UserId);
    }

    // UserDetails 인터페이스 구현
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(this.role));
    }
}
