package com.ayu.dadokim.business.med.ocr.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;

@Service
public class OcrService {

    private final RestTemplate restTemplate;
    private final ObjectMapper om = new ObjectMapper();

    public OcrService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    @Value("${clova.ocr.url}")
    private String ocrUrl;     // 반드시 개별 줄의 환경변수/설정으로 주입

    @Value("${clova.ocr.secret}")
    private String ocrSecret;  // X-OCR-SECRET

    public Map<String,Object> getConfig() {
        return Map.of(
                "urlPresent", ocrUrl != null && !ocrUrl.isBlank(),
                "secretPresent", ocrSecret != null && !ocrSecret.isBlank(),
                "urlScheme", ocrUrl == null ? null :
                        (ocrUrl.startsWith("https://") ? "https" :
                                (ocrUrl.startsWith("http://") ? "http" : "unknown"))
        );
    }

    public Map<String,Object> ping() {
        return Map.of("ok", true, "ts", System.currentTimeMillis());
    }

    public Map<String,Object> echo(MultipartFile file) {
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "no file");
        }
        return Map.of(
                "received", true,
                "name", file.getOriginalFilename(),
                "size", file.getSize(),
                "contentType", file.getContentType()
        );
    }

    public Map<String, Object> dnsCheck() throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("provider1", System.getProperty("sun.net.spi.nameservice.provider.1"));
        m.put("nameservers", System.getProperty("sun.net.spi.nameservice.nameservers"));
        var addrs = java.net.InetAddress.getAllByName("clovaocr-api-kr.ncloud.com");
        var list = new ArrayList<String>();
        for (var a : addrs) list.add(a.getHostAddress());
        m.put("resolved", list);
        return m;
    }

    public ResponseEntity<?> ocr(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file part missing");
        }
        if (ocrUrl == null || ocrUrl.isBlank() || ocrSecret == null || ocrSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OCR config missing");
        }

        // 이미지 → base64
        String fmt = "jpg";
        String name = file.getOriginalFilename();
        if (name != null && name.contains(".")) {
            String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
            if (ext.matches("png|jpg|jpeg|bmp|gif|webp")) {
                fmt = ext.equals("jpeg") ? "jpg" : ext;
            }
        }
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());

        Map<String,Object> imageMap = new HashMap<>();
        imageMap.put("format", fmt);
        imageMap.put("name", "prescription");
        imageMap.put("data", base64);

        Map<String,Object> body = new HashMap<>();
        body.put("version", "V2");
        body.put("requestId", UUID.randomUUID().toString());
        body.put("timestamp", System.currentTimeMillis());
        body.put("images", Collections.singletonList(imageMap));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-OCR-SECRET", ocrSecret);

        HttpEntity<Map<String,Object>> entity = new HttpEntity<>(body, headers);

        try {
            // CLOVA 원문 String으로 수신 → 에러메시지 그대로 돌려주기 위함
            ResponseEntity<String> resp = restTemplate.exchange(ocrUrl, HttpMethod.POST, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful()) {
                System.err.println("[CLOVA non-2xx] status=" + resp.getStatusCode() + " body=" + resp.getBody());
                return ResponseEntity.status(resp.getStatusCode())
                        .body(Map.of("error","CLOVA_NON_2XX",
                                "status",resp.getStatusCodeValue(),
                                "body",resp.getBody()));
            }

            Map<String,Object> json = om.readValue(resp.getBody(), new TypeReference<Map<String,Object>>() {});
            String text = extractInferText(json);
            return ResponseEntity.ok(Map.of("text", text, "raw", json));

        } catch (HttpClientErrorException e) {
            // CLOVA가 준 400/401/403 등의 본문을 그대로 반환
            String bodyStr = e.getResponseBodyAsString();
            System.err.println("[CLOVA 4xx] " + e.getStatusCode() + " body=" + bodyStr);
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error","CLOVA_4XX",
                            "status",e.getStatusCode().value(),
                            "body",bodyStr));
        } catch (RestClientException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error","UPSTREAM_FAILURE","message",e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error","SERVER_ERROR","message",e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private String extractInferText(Map<String,Object> root) {
        try {
            List<Object> images = (List<Object>) root.get("images");
            if (images == null || images.isEmpty()) return "";
            Map<String,Object> img0 = (Map<String,Object>) images.get(0);
            List<Object> fields = (List<Object>) img0.get("fields");
            if (fields == null) return "";

            StringBuilder sb = new StringBuilder();
            for (Object f0 : fields) {
                if (f0 instanceof Map) {
                    Map<?,?> f = (Map<?,?>) f0;
                    Object t = f.get("inferText");
                    Object c = f.get("inferConfidence");
                    double conf = (c instanceof Number) ? ((Number)c).doubleValue() : 1.0;

                    // 신뢰도 낮은 글자는 스킵 (예: 0.7 이하면 버림)
                    if (conf < 0.7) continue;

                    if (t != null) {
                        if (sb.length() > 0) sb.append('\n');
                        sb.append(String.valueOf(t));
                    }
                }
            }
            return sb.toString();
        } catch (Exception ignore) {
            return "";
        }
    }
}
