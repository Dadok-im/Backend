package com.ayu.dadokim.business.diary.service;

import com.ayu.dadokim.business.diary.form.DiaryEntity;
import com.ayu.dadokim.business.diary.form.request.DiaryRequest;
import com.ayu.dadokim.business.diary.form.response.DiaryListResponse;
import com.ayu.dadokim.business.diary.form.response.DiaryResponse;
import com.ayu.dadokim.business.diary.repository.DiaryRepository;
import com.ayu.dadokim.business.user.form.UserEntity;
import com.ayu.dadokim.business.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;

    @Transactional
    public DiaryResponse saveDiary(DiaryRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsernameAndIsLock(username, false)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        DiaryEntity diary = diaryRepository.findByUserAndDate(user, request.getDate())
                .map(d -> {
                    d.setDiaryText(request.getDiaryText());
                    d.setMood(request.getMood());
                    return diaryRepository.save(d);
                })
                .orElseGet(() -> diaryRepository.save(
                        DiaryEntity.builder()
                                .user(user)
                                .date(request.getDate())
                                .diaryText(request.getDiaryText())
                                .mood(request.getMood())
                                .build()
                ));

        return DiaryResponse.fromEntity(diary);
    }

    @Transactional(readOnly = true)
    public DiaryResponse getDiary(LocalDate date) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsernameAndIsLock(username, false)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return diaryRepository.findByUserAndDate(user, date)
                .map(DiaryResponse::fromEntity)
                .orElse(DiaryResponse.builder()
                        .exists(false)
                        .date(date)
                        .build());
    }

    @Transactional(readOnly = true)
    public List<DiaryListResponse> getAllDiaries() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsernameAndIsLock(username, false)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return diaryRepository.findAllByUser(user)
                .stream()
                .map(DiaryListResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
