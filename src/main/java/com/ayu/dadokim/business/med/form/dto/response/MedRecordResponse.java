package com.ayu.dadokim.business.med.form.dto.response;

public class MedRecordResponse {
    private Long id;
    private String name;
    private String source;
    private String date; // yyyy-MM-dd

    public MedRecordResponse(Long id, String name, String source, String date) {
        this.id = id;
        this.name = name;
        this.source = source;
        this.date = date;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSource() { return source; }
    public String getDate() { return date; }
}
