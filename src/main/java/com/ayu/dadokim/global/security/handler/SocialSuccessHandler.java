package com.ayu.dadokim.global.security.handler;

import com.ayu.dadokim.global.security.jwt.JwtUtil;
import com.ayu.dadokim.global.security.jwt.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Qualifier("SocialSuccessHandler")
public class SocialSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtService jwtService;
    private final JwtUtil jwtUtil;

    public SocialSuccessHandler(JwtService jwtService, JwtUtil jwtUtil) {
        this.jwtService = jwtService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // username, role
        String username =  authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // JWT(Refresh) 발급
        String refreshToken = jwtUtil.createJWT(username, "ROLE_" + role, false);

        // 발급한 Refresh DB 테이블 저장 (Refresh whitelist)
        jwtService.addRefresh(username, refreshToken);

//        // 응답
//        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
//        refreshCookie.setHttpOnly(true);
//        refreshCookie.setSecure(false);
//        refreshCookie.setPath("/");
//        refreshCookie.setMaxAge(10); // 10초 (프론트에서 발급 후 바로 헤더 전환 로직 진행 예정)
//
//        response.addCookie(refreshCookie);
        // response.sendRedirect("http://localhost:5173/cookie");

        // 🔹 SameSite / Secure 값을 설정에서 주입받아서 사용
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)            // local: false, prod: true
                .path("/")
                .maxAge(10)                      // 프론트에서 바로 /jwt/exchange 호출
                .sameSite("None")        // local: Lax, prod: None
                // .domain("your-domain.com")    // 나중에 도메인 생기면 여기서 공통 도메인 지정
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());

        response.sendRedirect("https://dadokim.netlify.app/cookie");
    }

}
