package com.ayu.dadokim.business.med.ocr.controller;

import com.ayu.dadokim.business.med.ocr.service.OcrService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OcrProxyController {

    private final OcrService ocrService;

    public OcrProxyController(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    // 설정 확인용(민감정보 마스킹)
    @GetMapping("/ocr/config")
    public Map<String,Object> config() {
        return ocrService.getConfig();
    }

    @GetMapping("/ocr/ping")
    public Map<String,Object> ping() {
        return ocrService.ping();
    }

    @PostMapping(value="/ocr/echo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,Object> echo(@RequestParam("file") MultipartFile file) {
        return ocrService.echo(file);
    }

    @GetMapping("/ocr/dns-check")
    public Map<String, Object> dnsCheck() throws Exception {
        return ocrService.dnsCheck();
    }

    @PostMapping(value="/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> ocr(@RequestParam("file") MultipartFile file) throws IOException {
        return ocrService.ocr(file);
    }
}
