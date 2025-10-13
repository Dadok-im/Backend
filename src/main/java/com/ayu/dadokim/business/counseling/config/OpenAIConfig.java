package com.ayu.dadokim.business.counseling.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIConfig {
    @Value("${chatkey}")
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }
}
