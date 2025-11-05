package com.ayu.dadokim.business.user.service;

import com.ayu.dadokim.business.user.form.Enum.SocialProviderType;
import com.ayu.dadokim.business.user.form.Enum.UserRoleType;
import com.ayu.dadokim.business.user.form.UserEntity;
import com.ayu.dadokim.business.user.form.request.CustomOAuth2User;
import com.ayu.dadokim.business.user.form.request.UserRequest;
import com.ayu.dadokim.business.user.form.response.UserResponse;
import com.ayu.dadokim.business.user.repository.UserRepository;
import com.ayu.dadokim.global.security.jwt.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService extends DefaultOAuth2UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * 1. 자체 로그인 회원가입 (회원 존재 여부 확인)
     */
    @Transactional(readOnly = true) // 트랜잭션 진행 시, 테이블 수정 없이 오로지 조회(readOnly) 가 목적임을 명시한다.
    public Boolean existUser(UserRequest request) {
        return userRepository.existsByUsername(request.getUsername());
    }

    /**
     * 2. 자체 로그인 회원가입 진행
     */
    @Transactional
    public Long addUser(UserRequest request) {
        // 1. 중복 유저 검사 진행
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("해당 유저가 이미 존재합니다.");
        }

        // 2. 중복 유저 검사 진행
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("해당 이메일으로 가입한 유저가 이미 존재합니다.");
        }

        // 3. 중복 유저가 아닐 경우, 회원 정보 값 entity 설정
        UserEntity userEntity = UserEntity.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .isLock(false)
                .isSocial(false)
                .roleType(UserRoleType.USER)
                .nickname(request.getNickname())
                .email(request.getEmail())
                .build();

        return userRepository.save(userEntity).getId();
    }

    /**
     * 3. 자체 로그인
     */
    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        UserEntity userEntity = userRepository.findByUsernameAndIsLockAndIsSocial(username, false, false)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        return User.builder()
                .username(userEntity.getUsername())
                .password(userEntity.getPassword())
                .roles(userEntity.getRoleType().name())
                .accountLocked(userEntity.getIsLock())
                .build();
    }

    /**
     * 4. 자체 로그인 회원 정보 수정 -> 로그인이 되어있는 경우 과연 인증이 필요할까?
     */
    @Transactional
    public Long updateUser(UserRequest request) throws AccessDeniedException {
        // 1. 현재 스레드에 로그인되어 있는 사용자의 이름을 가져온다.
        String sessionUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        // 2. 사용자의 이름이 같지 않다면, exception 을 던진다.
        if (!sessionUsername.equals(request.getUsername())) {
            throw new AccessDeniedException("본인의 계정만 수정이 가능합니다.");
        }
        // 3. 회원 조회
        UserEntity userEntity = userRepository.findByUsernameAndIsLockAndIsSocial(request.getUsername(), false, false)
                .orElseThrow(() -> new UsernameNotFoundException(request.getUsername()));
        // 4. 회원 정보 수정
        userEntity.updateUser(request);

        return userRepository.save(userEntity).getId();
    }

    /**
     *  5. 소셜 로그인 (로그인 시 : 신규 회원 = 가입, 기존 회원 = 업데이트)
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 1. 부모 메소드 호출
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. 데이터 파싱 필드 세팅
        Map<String, Object> attributes;
        List<GrantedAuthority> authorities;

        String username; // registration_id
        String role = UserRoleType.USER.name();
        String email; // naver mail address
        String nickname; // user name

        /**
         * error 해결 : nickname 값을 mock 값으로 넣으니 해결 !
         */
        // 3. provider 제공자별 데이터 획득
        String registrationId = userRequest.getClientRegistration().getRegistrationId().toUpperCase();

        // 4. 조건문에 따른 provider 별 데이터 값 파싱
        if (registrationId.equals(SocialProviderType.NAVER.name())) {

            // 4-1. naver OAuth2 서비스에서 넘어온 응답값이 올바른지 검증
            Object responseObj = oAuth2User.getAttributes().get("response");
            if (responseObj instanceof Map) {
                attributes = (Map<String, Object>) responseObj;
            } else {
                throw new OAuth2AuthenticationException("네이버 응답 데이터 형식이 잘못되었습니다.");
            }

            // 4-2. username 은 provider + provider_id 으로 파싱
            username = registrationId + "_" + attributes.get("id");

            // 4-3. email 파싱
            Object emailObj = attributes.get("email");
            email = (emailObj != null) ? emailObj.toString() : "";

            // 4-4. nickname 은 네이버 사용자의 name 으로 파싱
            Object nameObj = attributes.get("name");
            nickname = (nameObj != null) ? nameObj.toString() : "네이버사용자"; }

//       else if (registrationId.equals(SocialProviderType.KAKAO.name())) {
//
//            attributes = oAuth2User.getAttributes();
//
//            // 1. 고유 ID
//            String kakaoId = attributes.get("id").toString();
//            username = registrationId + "_" + kakaoId;
//
//            // 2. kakao_account 꺼내기
//            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
//
//            // 3. email (있을 수도 있고 없을 수도 있음 → null 체크 필요)
//            email = kakaoAccount.get("email") != null ? kakaoAccount.get("email").toString() : "";
//
//            // 4. profile 꺼내기
//            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
//            nickname = profile.get("nickname").toString();
//
//        }

        else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다.");
        }

        // 데이터베이스 조회 -> 존재하면 업데이트, 없으면 신규 가입
        Optional<UserEntity> entity = userRepository.findByUsernameAndIsSocial(username, true);
        // 1. 기존 회원이 존재하는 경우 -> update
        if (entity.isPresent()) {
            // role 조회
            role = entity.get().getRoleType().name();

            // 기존 유저 업데이트
            UserRequest dto = new UserRequest();
            dto.setNickname(nickname);
            dto.setEmail(email);
            entity.get().updateUser(dto);

            userRepository.save(entity.get());
        } else {
            // 신규 유저 추가
            UserEntity newUserEntity = UserEntity.builder()
                    .username(username)
                    .password("")
                    .isLock(false)
                    .isSocial(true)
                    .socialProviderType(SocialProviderType.valueOf(registrationId))
                    .roleType(UserRoleType.USER)
                    .nickname(nickname)
                    .email(email)
                    .build();

            userRepository.save(newUserEntity);
        }

        authorities = List.of(new SimpleGrantedAuthority(role));

        return new CustomOAuth2User(attributes, authorities, username);
    }

    /**
     *  6. 자체 / 소셜 로그인 회원 정보 조회
     */
    @Transactional(readOnly = true)
    public UserResponse readUser() {
        // 1. ContextHolder 를 통해 Session 에 존재하는 Username 을 조회
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2.
        UserEntity entity = userRepository.findByUsernameAndIsLock(username, false)
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을 수 없습니다: " + username));

        return new UserResponse(username, entity.getIsSocial(), entity.getNickname(), entity.getEmail());
    }

    /**
     *  7. 자체/소셜 로그인 회원 탈퇴
     */
    @Transactional
    public void deleteUser(UserRequest request) throws AccessDeniedException {

        // 본인 및 어드민만 삭제 가능 검증
        SecurityContext context = SecurityContextHolder.getContext();

        // 1. 현재 스레드가 가지고 있는 Session 의 username 과 Role 값을 받아온다.
        String sessionUsername = context.getAuthentication().getName();
        String sessionRole = context.getAuthentication().getAuthorities().iterator().next().getAuthority();

        // 2. requestDTO 의 값과 스레드 내의 Session 값 비교
        boolean isOwner = sessionUsername.equals(request.getUsername());
        boolean isAdmin = sessionRole.equals("ROLE_"+UserRoleType.ADMIN.name());

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("본인 혹은 관리자만 삭제할 수 있습니다.");
        }

        // 3. 인증 후 유저 제거
        userRepository.deleteByUsername(request.getUsername());

        // 4. JWT Refresh 토큰 제거
        jwtService.removeRefreshUser(request.getUsername());

    }
}
