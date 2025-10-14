package com.ayu.dadokim.global.config;


import com.ayu.dadokim.business.user.form.Enum.UserRoleType;
import com.ayu.dadokim.global.security.filter.JWTFilter;
import com.ayu.dadokim.global.security.filter.LoginFilter;
import com.ayu.dadokim.global.security.handler.RefreshTokenLogoutHandler;
import com.ayu.dadokim.global.security.jwt.JwtUtil;
import com.ayu.dadokim.global.security.jwt.service.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 securityfilterchain 은 프론트 -> 백엔드 api 요청 중 가장 먼저 통과하게 된다.
 **/

@Configuration
// @EnableWebSecurity(debug = true)
// @RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final AuthenticationSuccessHandler loginSuccessHandler;
    private final AuthenticationSuccessHandler socialSuccessHandler;
    private final JwtService jwtService;
    private final JwtUtil jwtUtil;

    public SecurityConfig(
            AuthenticationConfiguration authenticationConfiguration,
            // Bean 에 주입 받을 때, Qualifier 를 사용한다 -> Handler 구분에 용이
            @Qualifier("LoginSuccessHandler") AuthenticationSuccessHandler loginSuccessHandler,
            @Qualifier("SocialSuccessHandler") AuthenticationSuccessHandler socialSuccessHandler,
            JwtService jwtService,
            JwtUtil jwtUtil
    ) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.loginSuccessHandler = loginSuccessHandler;
        this.socialSuccessHandler = socialSuccessHandler;
        this.jwtService = jwtService;
        this.jwtUtil = jwtUtil;
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // 비밀번호 단방향(BCrypt) 암호화용 Bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // CORS Bean
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // 권한 계층 설정 (ADMIN 과 USER 간의 권한 계층을 설정하여 USER 권한에서 허용되는 API 는 ADMIN 권한에서도 허용이 된다는 것을 명시해준다.)
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withRolePrefix("ROLE_")
                .role(UserRoleType.ADMIN.name()).implies(UserRoleType.USER.name())
                .build();
    }

    // SecurityFilterChain 등록 Bean
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // CSRF 보안 필터 : disable
        http
                .csrf(AbstractHttpConfigurer::disable);
        // CORS 설정
        http
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource()));
        // 기본 Form 기반 인증 필터 : disable -> 직접 JSON 기반 LoginFilter 를 작성하여 커스텀 해준다.
        http
                .formLogin(AbstractHttpConfigurer::disable);
        // http basic 기반 인증 필터 : disable
        http
                .httpBasic(AbstractHttpConfigurer::disable);
        // 세션 필터 설정 (STATELESS)
        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));


        // OAuth2 인증용
        http
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(socialSuccessHandler));

        // 인가
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/jwt/exchange", "/jwt/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/logout").permitAll()

                        // UserController 매핑 반영
                        .requestMatchers(HttpMethod.POST, "/api/user/exist").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/user/join").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/clinics/search").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/gemini/").hasRole(UserRoleType.USER.name())
                        .requestMatchers(HttpMethod.POST, "/api/chat/").hasRole(UserRoleType.USER.name())
                        .requestMatchers(HttpMethod.GET, "/api/chat/history/all").hasRole(UserRoleType.USER.name())
//                        .requestMatchers(HttpMethod.GET, "/api/gemini/history/all").hasRole(UserRoleType.USER.name())
                        .requestMatchers(HttpMethod.GET, "/api/gemini/history/all").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/chat/history").hasRole(UserRoleType.USER.name())
                        .requestMatchers(HttpMethod.GET, "/api/gemini/history").hasRole(UserRoleType.USER.name())


                        .requestMatchers(HttpMethod.GET, "/api/user/me").hasRole(UserRoleType.USER.name())
                        .requestMatchers(HttpMethod.PUT, "/api/user/put").hasRole(UserRoleType.USER.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/user/delete").hasRole(UserRoleType.USER.name())

                        .anyRequest().authenticated()
                );


        // TODO: 로그인 관련 사용자 예외처리 --> 규격화된 에러 핸들러 처리 필요
        http
                .exceptionHandling(e -> e
                        // 1. 로그인 하지 않은 상태로 접근 했을 경우 : 401 에러
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        })
                        // 2. 접근 권한이 없는 상태로 접근 : 403 에러
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        })
                );


//        // 기본 로그아웃 필터 + 커스텀 Refresh 토큰 삭제 핸들러 추가
//        http
//                .logout(logout -> logout
//                        .addLogoutHandler(new RefreshTokenLogoutHandler(jwtService)));
        http
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .addLogoutHandler(new RefreshTokenLogoutHandler(jwtService, jwtUtil))
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\": \"Logout successful\"}");
                        })
                );

        // 커스텀된 JWT 필터를 LogoutFilter 앞단에 추가
        http
                .addFilterBefore(new JWTFilter(jwtUtil), LogoutFilter.class);

        // 커스텀 된 loginFilter 와 loginSuccessHandler 를 SecurityFilterChain 에 등록, 위치는 UsernamePasswordAuthenticationFilter 앞단
        http
                .addFilterBefore(new LoginFilter(authenticationManager(authenticationConfiguration), loginSuccessHandler), UsernamePasswordAuthenticationFilter.class);



        return http.build();
    }
}
