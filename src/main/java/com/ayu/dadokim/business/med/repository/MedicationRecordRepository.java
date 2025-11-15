// src/main/java/com/ayu/dadokim/business/med/MedicationRecordRepository.java
package com.ayu.dadokim.business.med.repository;

import com.ayu.dadokim.business.med.form.MedicationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MedicationRecordRepository extends JpaRepository<MedicationRecord, Long> {

    List<MedicationRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 날짜 범위 조회
    List<MedicationRecord> findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long userId, LocalDateTime start, LocalDateTime end
    );
}
