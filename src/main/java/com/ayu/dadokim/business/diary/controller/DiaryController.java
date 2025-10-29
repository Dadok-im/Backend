package com.ayu.dadokim.business.diary.controller;

import com.ayu.dadokim.business.diary.form.DiaryEntity;
import com.ayu.dadokim.business.diary.form.request.DiaryRequest;
import com.ayu.dadokim.business.diary.form.response.DiaryListResponse;
import com.ayu.dadokim.business.diary.form.response.DiaryResponse;
import com.ayu.dadokim.business.diary.service.DiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/diary")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    /** ✅ 일기 저장 또는 수정 */
    @PostMapping
    public ResponseEntity<DiaryResponse> saveDiary(@RequestBody DiaryRequest request) {
        DiaryResponse response = diaryService.saveDiary(request);
        return ResponseEntity.ok(response);
    }

    /** ✅ 특정 날짜 일기 조회 */
    @GetMapping
    public ResponseEntity<DiaryResponse> getDiary(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        DiaryResponse response = diaryService.getDiary(date);
        return ResponseEntity.ok(response);
    }

    /** ✅ 전체 일기 목록 조회 */
    @GetMapping("/list")
    public ResponseEntity<List<DiaryListResponse>> getAllDiaries() {
        List<DiaryListResponse> responses = diaryService.getAllDiaries();
        return ResponseEntity.ok(responses);
    }
}
