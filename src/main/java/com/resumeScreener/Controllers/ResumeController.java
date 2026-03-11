package com.resumeScreener.Controllers;


import com.resumeScreener.Repositories.AnalysisRepository;
import com.resumeScreener.Services.ResumeService;
import com.resumeScreener.entities.ResumeAnalysis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@CrossOrigin(origins = "*")
public class ResumeController {

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private AnalysisRepository analysisRepository;

    /**
     * Upload resume
     * form-data:
     *  - file: multipart file
     *  - jobId: long
     *  - candidateName: string
     *  - candidateEmail: string
     */
    @PostMapping("/upload")
    public ResumeAnalysis uploadResume(@RequestParam("file") MultipartFile file,
                                       @RequestParam("jobId") Long jobId,
                                       @RequestParam(value = "candidateName", required = false) String candidateName,
                                       @RequestParam(value = "candidateEmail", required = false) String candidateEmail) throws Exception {
        if (candidateName == null) candidateName = "Unknown";
        if (candidateEmail == null) candidateEmail = "";
        return resumeService.processResumeFile(file, jobId, candidateName, candidateEmail);
    }

    @GetMapping("/analysis/job/{jobId}")
    public List<ResumeAnalysis> analysesForJob(@PathVariable Long jobId) {
        return analysisRepository.findByJobPostingJobId(jobId);
    }

    @GetMapping("/analysis/{id}")
    public ResumeAnalysis getAnalysis(@PathVariable Long id) {
        return analysisRepository.findById(id).orElse(null);
    }

    @GetMapping("/all")
    public List<ResumeAnalysis> getAllAnalyses() {
        return analysisRepository.findAll();
    }
}

