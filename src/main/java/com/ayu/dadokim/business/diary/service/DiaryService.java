package com.ayu.dadokim.business.diary.service;

import com.ayu.dadokim.business.diary.form.DiaryEntity;
import com.ayu.dadokim.business.diary.repository.DiaryRepository;
import com.ayu.dadokim.business.user.form.UserEntity;
import com.ayu.dadokim.business.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;

    @Transactional
    public DiaryEntity saveDiary(LocalDate date, String text) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsernameAndIsLock(username, false)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return diaryRepository.findByUserAndDate(user, date)
                .map(diary -> {
                    diary.setDiaryText(text);
                    return diaryRepository.save(diary);
                })
                .orElseGet(() -> diaryRepository.save(
                        DiaryEntity.builder()
                                .user(user)
                                .date(date)
                                .diaryText(text)
                                .build()
                ));
    }

    @Transactional(readOnly = true)
    public DiaryEntity getDiary(LocalDate date) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsernameAndIsLock(username, false)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return diaryRepository.findByUserAndDate(user, date).orElse(null);
    }

    /**
     * ✅ 전체 일기 목록 조회
     */
    @Transactional(readOnly = true)
    public List<DiaryEntity> getAllDiaries() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsernameAndIsLock(username, false)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return diaryRepository.findAllByUser(user);
    }
}
