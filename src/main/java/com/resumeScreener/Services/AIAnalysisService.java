package com.resumeScreener.Services;

import com.resumeScreener.dto.ResumeReviewResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class AIAnalysisService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent";

    public JSONObject analyze(String jobDescription, String resumeText) {
        try {
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

            return executePrompt(prompt);
        } catch (Exception e) {
            return fallback("Error calling Gemini: " + e.getMessage());
        }
    }

    public ResumeReviewResponse reviewResume(String targetRole, String candidateName, String resumeText) {
        String normalizedRole = (targetRole == null || targetRole.isBlank()) ? "General professional role" : targetRole;
        String normalizedName = (candidateName == null || candidateName.isBlank()) ? "Candidate" : candidateName;

        String prompt = """
                You are an expert resume reviewer.
                Review the following resume for the target role.
                Return ONLY a JSON object with keys:
                - overallScore (0-100 integer)
                - strengths (array of strings, 3 to 5 points)
                - improvements (array of strings, 3 to 5 points)
                - missingSkills (array of strings)
                - summary (string, 2 to 4 sentences)

                Candidate Name:
                %s

                Target Role:
                %s

                Resume Text:
                %s
                """.formatted(normalizedName, normalizedRole, resumeText);

        try {
            JSONObject result = executePrompt(prompt);

            ResumeReviewResponse response = new ResumeReviewResponse();
            response.setCandidateName(normalizedName);
            response.setTargetRole(normalizedRole);
            response.setOverallScore(result.optDouble("overallScore", result.optDouble("matchScore", 0)));
            response.setStrengths(toList(result.optJSONArray("strengths")));
            response.setImprovements(toList(result.optJSONArray("improvements")));
            response.setMissingSkills(toList(result.optJSONArray("missingSkills")));
            response.setSummary(result.optString("summary", result.optString("remarks", "No summary available.")));
            return response;
        } catch (Exception e) {
            ResumeReviewResponse response = new ResumeReviewResponse();
            response.setCandidateName(normalizedName);
            response.setTargetRole(normalizedRole);
            response.setOverallScore(0);
            response.setStrengths(List.of("Unable to extract strengths right now."));
            response.setImprovements(List.of("Try again after confirming the AI service configuration."));
            response.setMissingSkills(List.of());
            response.setSummary("Review failed: " + e.getMessage());
            return response;
        }
    }

    private JSONObject fallback(String msg) {
        JSONObject obj = new JSONObject();
        obj.put("matchScore", 0);
        obj.put("missingSkills", new JSONArray());
        obj.put("remarks", msg);
        return obj;
    }

    private JSONObject executePrompt(String prompt) {
        RestTemplate rest = new RestTemplate();

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

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new IllegalStateException("Gemini returned non-OK: " + response.getStatusCode());
        }

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
    }

    private List<String> toList(JSONArray array) {
        List<String> values = new ArrayList<>();
        if (array == null) {
            return values;
        }

        for (int i = 0; i < array.length(); i++) {
            values.add(array.optString(i));
        }
        return values;
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
