package com.ayu.dadokim.business.diary.controller;

import com.ayu.dadokim.business.diary.form.DiaryEntity;
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

    /**
     * ✅ 일기 저장 또는 수정
     * POST /api/diary
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> saveDiary(@RequestBody Map<String, String> body) {
        LocalDate date = LocalDate.parse(body.get("date"));
        String text = body.get("diaryText");
        DiaryEntity diary = diaryService.saveDiary(date, text);

        return ResponseEntity.ok(Map.of(
                "id", diary.getId(),
                "date", diary.getDate(),
                "diaryText", diary.getDiaryText()
        ));
    }

    /**
     * ✅ 특정 날짜 일기 조회
     * GET /api/diary?date=2025-01-03
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDiary(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        DiaryEntity diary = diaryService.getDiary(date);
        if (diary == null)
            return ResponseEntity.ok(Map.of("exists", false));

        return ResponseEntity.ok(Map.of(
                "exists", true,
                "date", diary.getDate(),
                "diaryText", diary.getDiaryText()
        ));
    }

    /**
     * ✅ 전체 일기 목록 조회
     * GET /api/diary/list
     */
    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> getAllDiaries() {
        List<DiaryEntity> diaries = diaryService.getAllDiaries();

        List<Map<String, Object>> response = diaries.stream()
                .map(d -> Map.<String, Object>of(
                        "date", d.getDate(),
                        "diaryText", d.getDiaryText()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

}
