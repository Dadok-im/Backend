package com.ayu.dadokim.business.counseling.form;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor; // Lombok's constructor annotation

@Getter
@Setter
@AllArgsConstructor // This creates a constructor with all fields as arguments
public class ChatResponseDTO {
    private String role;
    private String reply;
}