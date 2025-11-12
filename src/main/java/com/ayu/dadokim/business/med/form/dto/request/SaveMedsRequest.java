package com.ayu.dadokim.business.med.form.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SaveMedsRequest {
    private List<String> names; // 약 이름 목록
    private String source;      // "ocr" 또는 "manual"
}
