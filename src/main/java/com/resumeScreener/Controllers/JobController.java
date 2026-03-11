package com.resumeScreener.Controllers;



import com.resumeScreener.Repositories.JobRepository;
import com.resumeScreener.entities.JobPosting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    @Autowired
    private JobRepository jobRepository;

    @PostMapping
    public JobPosting createJob(@RequestBody JobPosting job) {
        return jobRepository.save(job);
    }

    @GetMapping
    public List<JobPosting> listJobs() {
        return jobRepository.findAll();
    }

    @GetMapping("/{jobId}")
    public JobPosting getJob(@PathVariable Long jobId) {
        return jobRepository.findById(jobId).orElse(null);
    }
}

