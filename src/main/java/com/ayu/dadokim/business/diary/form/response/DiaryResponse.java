package com.ayu.dadokim.business.diary.form.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class DiaryResponse {

    private Long id;
    private LocalDate date;
    private String diaryText;
    private String mood;

    private boolean exists; // 단건 조회 시 존재 여부 표시용

    public static DiaryResponse fromEntity(com.ayu.dadokim.business.diary.form.DiaryEntity diary) {
        return DiaryResponse.builder()
                .id(diary.getId())
                .date(diary.getDate())
                .diaryText(diary.getDiaryText())
                .mood(diary.getMood())
                .exists(true)
                .build();
    }
}
