package com.ayu.dadokim.business.diary.repository;

import com.ayu.dadokim.business.diary.form.DiaryEntity;
import com.ayu.dadokim.business.user.form.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<DiaryEntity, Long> {
    Optional<DiaryEntity> findByUserAndDate(UserEntity user, LocalDate date);
}