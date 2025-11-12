package com.ayu.dadokim.business.med.form;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "medication_record")
public class MedicationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 로그인 유저 id
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;     // 예: "화록소정"

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true, length = 20)
    private String source;   // 예: "ocr", "manual"

    public MedicationRecord() {}

    public MedicationRecord(Long userId, String name, String source) {
        this.userId = userId;
        this.name = name;
        this.source = source;
        this.createdAt = LocalDateTime.now();
    }

    // getter / setter

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
