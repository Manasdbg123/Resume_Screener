package com.resumeScreener.Services;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AIAnalysisService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent";

    public JSONObject analyze(String jobDescription, String resumeText) {
        try {
            RestTemplate rest = new RestTemplate();

            String prompt = """
                You are an AI assistant that compares a job description and a candidate resume.
                Return ONLY a JSON object with keys:
                - matchScore (0-100 integer)
                - missingSkills (array of strings)
                - remarks (string short summary)
                Use job description and resume below.

                Job Description:
                %s

                Resume Text:
                %s
                """.formatted(jobDescription, resumeText);

            JSONObject contents = new JSONObject()
                    .put("contents", new JSONArray()
                            .put(new JSONObject()
                                    .put("parts", new JSONArray()
                                            .put(new JSONObject().put("text", prompt)))));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String urlWithKey = GEMINI_URL + "?key=" + geminiApiKey;

            HttpEntity<String> request = new HttpEntity<>(contents.toString(), headers);
            ResponseEntity<String> response = rest.postForEntity(urlWithKey, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JSONObject respJson = new JSONObject(response.getBody());
                String content = respJson
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");

                String jsonString = extractJson(content);
                return new JSONObject(jsonString);
            } else {
                return fallback("Gemini returned non-OK: " + response.getStatusCode());
            }
        } catch (Exception e) {
            return fallback("Error calling Gemini: " + e.getMessage());
        }
    }

    private JSONObject fallback(String msg) {
        JSONObject obj = new JSONObject();
        obj.put("matchScore", 0);
        obj.put("missingSkills", new JSONArray());
        obj.put("remarks", msg);
        return obj;
    }

    private String extractJson(String content) {
        content = content.trim();
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }
}
