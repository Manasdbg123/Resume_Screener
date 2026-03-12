package com.resumeScreener.dto;

import java.util.List;

public class ResumeReviewResponse {

    private String candidateName;
    private String targetRole;
    private double overallScore;
    private List<String> strengths;
    private List<String> improvements;
    private List<String> missingSkills;
    private String summary;
    private String extractedTextPreview;

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(double overallScore) {
        this.overallScore = overallScore;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getImprovements() {
        return improvements;
    }

    public void setImprovements(List<String> improvements) {
        this.improvements = improvements;
    }

    public List<String> getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(List<String> missingSkills) {
        this.missingSkills = missingSkills;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getExtractedTextPreview() {
        return extractedTextPreview;
    }

    public void setExtractedTextPreview(String extractedTextPreview) {
        this.extractedTextPreview = extractedTextPreview;
    }
}
