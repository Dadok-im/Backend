package com.ayu.dadokim.business.diary.form.request;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class DiaryRequest {

    private LocalDate date;
    private String diaryText;
}
