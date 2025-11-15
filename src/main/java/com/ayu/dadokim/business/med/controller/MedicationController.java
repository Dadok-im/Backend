package com.ayu.dadokim.business.med.controller;

import com.ayu.dadokim.business.med.form.MedicationRecord;
import com.ayu.dadokim.business.med.form.dto.response.MedRecordResponse;
import com.ayu.dadokim.business.med.form.dto.request.SaveMedsRequest;
import com.ayu.dadokim.business.med.service.MedicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medications")
public class MedicationController {

    private final MedicationService medicationService;

    public MedicationController(MedicationService medicationService) {
        this.medicationService = medicationService;
    }

    // 약 저장 (OCR / 수동 공통)
    @PostMapping
    public ResponseEntity<List<MedicationRecord>> saveMedications(
            @RequestBody SaveMedsRequest req
    ) {
        return ResponseEntity.ok(medicationService.saveMedications(req));
    }

    // 특정 날짜 약 목록
    @GetMapping("/by-date")
    public List<MedRecordResponse> getByDate(@RequestParam String date) {
        return medicationService.getByDate(date);
    }

    // 약이 있는 날짜들
    @GetMapping("/days")
    public List<String> getDaysWithMeds(
            @RequestParam String from,
            @RequestParam String to
    ) {
        return medicationService.getDaysWithMeds(from, to);
    }

    // 약 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMedication(@PathVariable Long id) {
        medicationService.deleteMedication(id);
        return ResponseEntity.ok("약물이 삭제되었습니다.");
    }

    // 약 수정 (이름/소스)
    @PutMapping("/{id}")
    public ResponseEntity<MedicationRecord> updateMedication(
            @PathVariable Long id,
            @RequestBody MedicationRecord body
    ) {
        MedicationRecord updated =
                medicationService.updateMedication(id, body.getName(), body.getSource());
        return ResponseEntity.ok(updated);
    }
}
