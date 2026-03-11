package com.resumeScreener.Services;


import com.resumeScreener.Repositories.AnalysisRepository;
import com.resumeScreener.Repositories.CandidateRepository;
import com.resumeScreener.Repositories.JobRepository;
import com.resumeScreener.entities.Candidate;
import com.resumeScreener.entities.JobPosting;
import com.resumeScreener.entities.ResumeAnalysis;
import org.apache.tika.Tika;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeService {

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private AnalysisRepository analysisRepository;

    @Autowired
    private AIAnalysisService aiService;

    private final Tika tika = new Tika();

    /**
     * Process a resume file for a specific job.
     */
    public ResumeAnalysis processResumeFile(MultipartFile file, Long jobId, String candidateName, String candidateEmail) throws Exception {
        // 1. find job
        JobPosting job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        // 2. extract text from file
        String resumeText = tika.parseToString(file.getInputStream());

        // 3. save candidate
        Candidate candidate = new Candidate();
        candidate.setName(candidateName);
        candidate.setEmail(candidateEmail);
        candidate.setResumeText(resumeText);
        candidate = candidateRepository.save(candidate);

        // 4. call AI analysis
        JSONObject aiResponse = aiService.analyze(job.getDescription(), resumeText);

        // 5. map results to ResumeAnalysis entity
        ResumeAnalysis analysis = new ResumeAnalysis();
        analysis.setJobPosting(job);
        analysis.setCandidate(candidate);

        // matchScore might be integer/double
        double score = 0.0;
        try {
            score = aiResponse.optDouble("matchScore", aiResponse.optInt("matchScore", 0));
        } catch (Exception ignore) {}

        analysis.setMatchScore(score);

        JSONArray missing = aiResponse.optJSONArray("missingSkills");
        if (missing != null) {
            analysis.setMissingSkills(missing.toString());
        } else {
            analysis.setMissingSkills(aiResponse.optString("missingSkills", ""));
        }

        analysis.setRemarks(aiResponse.optString("remarks", ""));

        // 6. save analysis
        analysis = analysisRepository.save(analysis);

        return analysis;
    }
}

