package com.ayu.dadokim.business.diary.form.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class DiaryListResponse {
    private LocalDate date;
    private String diaryText;
    private String mood;

    public static DiaryListResponse fromEntity(com.ayu.dadokim.business.diary.form.DiaryEntity diary) {
        return DiaryListResponse.builder()
                .date(diary.getDate())
                .diaryText(diary.getDiaryText())
                .mood(diary.getMood())
                .build();
    }
}
