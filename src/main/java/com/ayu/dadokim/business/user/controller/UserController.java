package com.ayu.dadokim.business.user.controller;

import com.ayu.dadokim.business.user.form.request.UserRequest;
import com.ayu.dadokim.business.user.form.response.UserResponse;
import com.ayu.dadokim.business.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

/**
 * 📘 UserController
 * ----------------------------
 * 사용자 회원 관리 기능을 제공하는 REST API 컨트롤러입니다.
 *
 * 주요 기능:
 * 1. 자체 로그인 사용자 존재 여부 확인
 * 2. 회원가입 (자체 로그인)
 * 3. 로그인된 사용자 정보 조회
 * 4. 회원 정보 수정 (자체 로그인 유저만)
 * 5. 회원 탈퇴 (자체/소셜 로그인 모두)
 *
 * 모든 요청은 `/api/user` prefix를 가집니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    /**
     * 🔍 자체 로그인 사용자 존재 여부 확인
     * -----------------------------------
     * 입력된 이메일(또는 아이디)을 기반으로
     * 해당 사용자가 이미 존재하는지 확인합니다.
     *
     * ✅ 요청
     * - URL: {@code POST /api/user/exist}
     * - Content-Type: {@code application/json}
     * - Body: {@link UserRequest} (existGroup 검증 그룹)
     *
     * ✅ 응답
     * - {@code true} : 이미 존재함
     * - {@code false}: 존재하지 않음
     *
     * ✅ 인증
     * - 불필요 (회원가입 전 단계)
     */
    @PostMapping(value = "/exist", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> existUserApi(
            @Validated(UserRequest.existGroup.class) @RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.existUser(request));
    }

    /**
     * 📝 회원가입 (자체 로그인)
     * ----------------------------
     * 신규 사용자를 등록하고 생성된 사용자 ID를 반환합니다.
     *
     * ✅ 요청
     * - URL: {@code POST /api/user/join}
     * - Content-Type: {@code application/json}
     * - Body: {@link UserRequest} (addGroup 검증 그룹)
     *
     * ✅ 응답
     * - HTTP 201 Created
     * - Body: {"userEntityId": Long}
     *
     * ✅ 인증
     * - 불필요 (회원가입 단계)
     */
    @PostMapping(value = "/join", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> joinApi(
            @Validated(UserRequest.addGroup.class) @RequestBody UserRequest request) {

        Long id = userService.addUser(request);
        Map<String, Long> responseBody = Collections.singletonMap("userEntityId", id);

        return ResponseEntity.status(201).body(responseBody);
    }

    /**
     * 👤 로그인된 사용자 정보 조회
     * ----------------------------
     * 현재 JWT 토큰으로 인증된 사용자의 정보를 반환합니다.
     *
     * ✅ 요청
     * - URL: {@code GET /api/user/me}
     *
     * ✅ 응답
     * - {@link UserResponse} (사용자 상세 정보)
     *
     * ✅ 인증
     * - 필수 (JWT 기반)
     *
     * ⚠️ 주의
     * - GET 요청 시 Content-Type 헤더를 함께 보내면 안 됩니다.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> userMeApi() {
        return ResponseEntity.ok(userService.readUser());
    }

    /**
     * ✏️ 사용자 정보 수정 (자체 로그인 유저만)
     * ---------------------------------------
     * 사용자 프로필 정보를 수정합니다.
     *
     * ✅ 요청
     * - URL: {@code PUT /api/user/put}
     * - Content-Type: {@code application/json}
     * - Body: {@link UserRequest} (updateGroup 검증 그룹)
     *
     * ✅ 응답
     * - HTTP 200 OK
     * - Body: 수정된 사용자 ID (Long)
     *
     * ✅ 인증
     * - 필수 (JWT 기반)
     * - 자체 로그인 유저만 허용
     *
     * @throws AccessDeniedException 소셜 로그인 유저가 접근 시
     */
    @PutMapping("/put")
    public ResponseEntity<Long> updateUserApi(
            @Validated(UserRequest.updateGroup.class) @RequestBody UserRequest request) throws AccessDeniedException {
        return ResponseEntity.ok(userService.updateUser(request));
    }

    /**
     * ❌ 회원 탈퇴 (자체/소셜 유저 모두)
     * -----------------------------------
     * 계정을 완전히 삭제합니다.
     *
     * ✅ 요청
     * - URL: {@code DELETE /api/user/delete}
     * - Content-Type: {@code application/json}
     * - Body: {@link UserRequest} (deleteGroup 검증 그룹)
     *
     * ✅ 응답
     * - HTTP 204 No Content (삭제 완료)
     *
     * ✅ 인증
     * - 필수 (JWT 기반)
     *
     * @throws AccessDeniedException 권한 없는 사용자가 접근 시
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteUserApi(
            @Validated(UserRequest.deleteGroup.class) @RequestBody UserRequest request) throws AccessDeniedException {
        userService.deleteUser(request);
        return ResponseEntity.noContent().build(); // ✅ 삭제 성공 시 204 No Content
    }
}
