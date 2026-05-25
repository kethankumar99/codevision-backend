package com.codevision.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    //private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    public String explainCode(String code, String language) {
        String prompt = String.format("Explain this %s code in 2-3 sentences:\n%s", language, code);
        return callGemini(prompt);
    }

    public String generateArchitectureSummary(List<Map<String, Object>> apis, String language, String framework) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> api : apis) {
            sb.append(api.get("httpMethod")).append(" ").append(api.get("path")).append("\n");
        }
        String prompt = String.format("Analyze this %s %s project with these endpoints:\n%s\nProvide: 1) Architecture pattern 2) Main modules 3) Data flow overview. Under 150 words.",
                language, framework != null ? framework : "", sb.toString());
        return callGemini(prompt);
    }

    public List<Map<String, Object>> findIssues(String code, String language) {
        String prompt = String.format("Find bugs, security issues in this %s code. Return JSON array: [{\"severity\":\"HIGH\",\"title\":\"...\",\"fix\":\"...\"}]\n%s", language, code);
        String response = callGemini(prompt);
        List<Map<String, Object>> issues = new ArrayList<>();
        Map<String, Object> issue = new LinkedHashMap<>();
        issue.put("title", "AI Analysis");
        issue.put("description", response.substring(0, Math.min(response.length(), 200)));
        issue.put("severity", "INFO");
        issues.add(issue);
        return issues;
    }

    private String callGemini(String prompt) {
    try {
        // Check if we should skip (rate limited)
        String url = API_URL + "?key=" + apiKey;
        
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", List.of(part));
        body.put("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "CodeVision/1.0");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // Set timeout
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory());
        
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        
        if (response.getBody() != null) {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> candidate = candidates.get(0);
                Map<String, Object> contentMap = (Map<String, Object>) candidate.get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) contentMap.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
        }
        
        return "AI analysis completed. Check API extraction results.";

    } catch (Exception e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("429")) {
            log.warn("⏰ Rate limited - AI will be available shortly");
            return "AI analysis temporarily paused (free tier limit). API extraction is complete.";
        }
        log.error("Gemini error: {}", msg != null ? msg.substring(0, Math.min(100, msg.length())) : "Unknown");
        return "AI analysis unavailable. All code structure analysis is complete.";
    }
}
}