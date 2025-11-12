package com.ayu.dadokim.business.med.service;

import com.ayu.dadokim.business.med.form.MedicationRecord;
import com.ayu.dadokim.business.med.form.dto.response.MedRecordResponse;
import com.ayu.dadokim.business.med.form.dto.request.SaveMedsRequest;
import com.ayu.dadokim.business.med.repository.MedicationRecordRepository;
import com.ayu.dadokim.business.user.form.UserEntity;
import com.ayu.dadokim.business.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicationService {

    private final MedicationRecordRepository repo;
    private final UserRepository userRepository;

    public MedicationService(MedicationRecordRepository repo, UserRepository userRepository) {
        this.repo = repo;
        this.userRepository = userRepository;
    }

    // 현재 로그인 유저 ID
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        UserEntity user = userRepository.findByUsernameAndIsLock(username, false)
                .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다: " + username));
        return user.getId();
    }

    // 약 저장
    public List<MedicationRecord> saveMedications(SaveMedsRequest req) {
        Long userId = getCurrentUserId();
        String source = (req.getSource() == null || req.getSource().isBlank())
                ? "manual" : req.getSource();

        return req.getNames().stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .map(name -> new MedicationRecord(userId, name, source))
                .map(repo::save)
                .collect(Collectors.toList());
    }

    // 특정 날짜 약 조회
    public List<MedRecordResponse> getByDate(String date) {
        Long userId = getCurrentUserId();
        LocalDate d = LocalDate.parse(date);
        LocalDateTime start = d.atStartOfDay();
        LocalDateTime end = d.plusDays(1).atStartOfDay();

        return repo.findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(userId, start, end)
                .stream()
                .map(m -> new MedRecordResponse(
                        m.getId(),
                        m.getName(),
                        m.getSource(),
                        m.getCreatedAt().toLocalDate().toString()
                ))
                .collect(Collectors.toList());
    }

    // 약이 있는 날짜 리스트
    public List<String> getDaysWithMeds(String from, String to) {
        Long userId = getCurrentUserId();
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);

        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.plusDays(1).atStartOfDay();

        return repo.findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(userId, start, end)
                .stream()
                .map(m -> m.getCreatedAt().toLocalDate().toString())
                .distinct()
                .collect(Collectors.toList());
    }

    // 약 삭제
    public void deleteMedication(Long id) {
        Long userId = getCurrentUserId();
        MedicationRecord med = repo.findById(id)
                .filter(m -> m.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("약물을 찾을 수 없습니다."));
        repo.delete(med);
    }

    // 약 수정
    public MedicationRecord updateMedication(Long id, String newName, String newSource) {
        Long userId = getCurrentUserId();
        MedicationRecord med = repo.findById(id)
                .filter(m -> m.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("약물을 찾을 수 없습니다."));

        med.setName(newName);
        if (newSource != null && !newSource.isBlank()) {
            med.setSource(newSource);
        }
        med.setCreatedAt(LocalDateTime.now());

        return repo.save(med);
    }
}
