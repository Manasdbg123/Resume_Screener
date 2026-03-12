package com.resumeScreener.Controllers;



import com.resumeScreener.Repositories.JobRepository;
import com.resumeScreener.Services.AuthService;
import com.resumeScreener.entities.AppUser;
import com.resumeScreener.entities.JobPosting;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private AuthService authService;

    @PostMapping
    public JobPosting createJob(@RequestBody JobPosting job, HttpSession session) {
        AppUser user;
        try {
            user = authService.requireAuthenticatedUser(session);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage());
        }

        if (!"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin users can publish jobs.");
        }

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

