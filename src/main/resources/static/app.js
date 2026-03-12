const state = {
    jobs: [],
    analyses: [],
    auth: {
        authenticated: false,
        role: "ANONYMOUS"
    }
};

const jobsList = document.getElementById("jobs-list");
const jobsEmpty = document.getElementById("jobs-empty");
const jobsCount = document.getElementById("jobs-count");
const analysesCount = document.getElementById("analyses-count");
const applicationJob = document.getElementById("application-job");
const resultsSection = document.getElementById("results-section");
const authBadge = document.getElementById("auth-badge");
const authSummary = document.getElementById("auth-summary");
const logoutButton = document.getElementById("logout-button");
const jobAuthHint = document.getElementById("job-auth-hint");
const jobForm = document.getElementById("job-form");

const tabButtons = Array.from(document.querySelectorAll(".tab-button"));
const tabPanels = Array.from(document.querySelectorAll(".tab-panel"));
const authTabButtons = Array.from(document.querySelectorAll(".auth-tab"));
const authPanels = Array.from(document.querySelectorAll(".auth-panel"));

document.getElementById("refresh-jobs").addEventListener("click", loadDashboard);
document.getElementById("job-form").addEventListener("submit", handleJobCreate);
document.getElementById("review-form").addEventListener("submit", handleGeneralReview);
document.getElementById("job-application-form").addEventListener("submit", handleJobApplication);
document.getElementById("login-form").addEventListener("submit", handleLogin);
document.getElementById("signup-form").addEventListener("submit", handleSignup);
logoutButton.addEventListener("click", handleLogout);

tabButtons.forEach((button) => {
    button.addEventListener("click", () => {
        const target = button.dataset.tab;
        tabButtons.forEach((item) => item.classList.toggle("active", item === button));
        tabPanels.forEach((panel) => panel.classList.toggle("active", panel.id === target));
    });
});

authTabButtons.forEach((button) => {
    button.addEventListener("click", () => {
        const target = button.dataset.authTab;
        authTabButtons.forEach((item) => item.classList.toggle("active", item === button));
        authPanels.forEach((panel) => panel.classList.toggle("active", panel.id === target));
    });
});

initialize();

async function initialize() {
    await Promise.all([loadAuthState(), loadDashboard()]);
    updateAuthUI();
}

async function loadDashboard() {
    await Promise.all([loadJobs(), loadAnalyses()]);
}

async function loadAuthState() {
    try {
        const response = await fetch("/api/auth/me");
        if (!response.ok) {
            throw new Error("Unable to load auth state.");
        }

        state.auth = await response.json();
    } catch (error) {
        state.auth = { authenticated: false, role: "ANONYMOUS" };
    }
}

async function loadJobs() {
    try {
        const response = await fetch("/api/jobs");
        if (!response.ok) {
            throw new Error("Unable to load jobs.");
        }

        state.jobs = await response.json();
        renderJobs();
        populateJobSelect();
    } catch (error) {
        jobsEmpty.textContent = error.message;
        jobsEmpty.classList.remove("hidden");
        jobsList.innerHTML = "";
        applicationJob.innerHTML = '<option value="">No jobs available</option>';
        jobsCount.textContent = "0";
    }
}

async function loadAnalyses() {
    try {
        const response = await fetch("/api/resumes/all");
        if (!response.ok) {
            throw new Error("Unable to load analyses.");
        }

        state.analyses = await response.json();
        analysesCount.textContent = String(state.analyses.length);
    } catch (error) {
        analysesCount.textContent = "0";
    }
}

function renderJobs() {
    jobsCount.textContent = String(state.jobs.length);
    jobsList.innerHTML = "";

    if (!state.jobs.length) {
        jobsEmpty.textContent = "No jobs published yet. Add one below to start receiving role-specific CV analysis.";
        jobsEmpty.classList.remove("hidden");
        return;
    }

    jobsEmpty.classList.add("hidden");
    state.jobs.forEach((job) => {
        const card = document.createElement("article");
        card.className = "job-card";

        const skills = splitSkills(job.requiredSkills);
        const skillMarkup = skills.length
            ? `<div class="skills-row">${skills.map((skill) => `<span class="skill-chip">${escapeHtml(skill)}</span>`).join("")}</div>`
            : '<div class="job-meta">No required skills listed.</div>';

        card.innerHTML = `
            <p class="eyebrow">Job #${job.jobId}</p>
            <h3>${escapeHtml(job.title || "Untitled role")}</h3>
            <p class="job-meta">${formatDate(job.createdAt)}</p>
            <p>${escapeHtml(trimText(job.description || "", 220))}</p>
            ${skillMarkup}
            <button class="button button-secondary" type="button" data-apply-job="${job.jobId}">Apply to this job</button>
        `;

        card.querySelector("[data-apply-job]").addEventListener("click", () => {
            applicationJob.value = String(job.jobId);
            document.querySelector('[data-tab="job-review"]').click();
            document.getElementById("candidate-tools").scrollIntoView({ behavior: "smooth", block: "start" });
        });

        jobsList.appendChild(card);
    });
}

function populateJobSelect() {
    applicationJob.innerHTML = "";

    if (!state.jobs.length) {
        applicationJob.innerHTML = '<option value="">No jobs available</option>';
        return;
    }

    const placeholder = document.createElement("option");
    placeholder.value = "";
    placeholder.textContent = "Select a job";
    applicationJob.appendChild(placeholder);

    state.jobs.forEach((job) => {
        const option = document.createElement("option");
        option.value = String(job.jobId);
        option.textContent = `${job.title || "Untitled role"} (#${job.jobId})`;
        applicationJob.appendChild(option);
    });
}

async function handleJobCreate(event) {
    event.preventDefault();
    const status = document.getElementById("job-status");
    const submit = document.getElementById("job-submit");

    if (!state.auth.authenticated) {
        status.textContent = "Login first.";
        return;
    }

    const payload = {
        title: document.getElementById("job-title").value.trim(),
        requiredSkills: document.getElementById("job-skills").value.trim(),
        description: document.getElementById("job-description").value.trim()
    };

    submit.disabled = true;
    status.textContent = "Publishing job...";

    try {
        const response = await fetch("/api/jobs", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
            credentials: "same-origin"
        });

        if (!response.ok) {
            throw new Error(await response.text() || "Could not publish job.");
        }

        event.target.reset();
        status.textContent = "Job published.";
        await loadJobs();
    } catch (error) {
        status.textContent = error.message;
    } finally {
        submit.disabled = false;
    }
}

async function handleLogin(event) {
    event.preventDefault();
    const status = document.getElementById("login-status");
    const submit = document.getElementById("login-submit");

    submit.disabled = true;
    status.textContent = "Logging in...";

    try {
        const response = await fetch("/api/auth/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                email: document.getElementById("login-email").value.trim(),
                password: document.getElementById("login-password").value
            })
        });

        if (!response.ok) {
            throw new Error(await response.text() || "Login failed.");
        }

        state.auth = await response.json();
        status.textContent = "Logged in.";
        updateAuthUI();
    } catch (error) {
        status.textContent = error.message;
    } finally {
        submit.disabled = false;
    }
}

async function handleSignup(event) {
    event.preventDefault();
    const status = document.getElementById("signup-status");
    const submit = document.getElementById("signup-submit");

    submit.disabled = true;
    status.textContent = "Creating account...";

    try {
        const response = await fetch("/api/auth/signup", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                name: document.getElementById("signup-name").value.trim(),
                email: document.getElementById("signup-email").value.trim(),
                password: document.getElementById("signup-password").value
            })
        });

        if (!response.ok) {
            throw new Error(await response.text() || "Signup failed.");
        }

        state.auth = await response.json();
        status.textContent = "Account created and logged in.";
        updateAuthUI();
    } catch (error) {
        status.textContent = error.message;
    } finally {
        submit.disabled = false;
    }
}

async function handleLogout() {
    await fetch("/api/auth/logout", { method: "POST" });
    state.auth = { authenticated: false, role: "ANONYMOUS" };
    updateAuthUI();
}

async function handleGeneralReview(event) {
    event.preventDefault();
    const submit = document.getElementById("review-submit");
    const status = document.getElementById("review-status");
    const fileInput = document.getElementById("review-file");

    if (!fileInput.files.length) {
        status.textContent = "Choose a resume file first.";
        return;
    }

    const formData = new FormData();
    formData.append("file", fileInput.files[0]);
    formData.append("candidateName", document.getElementById("candidateName").value.trim());
    formData.append("targetRole", document.getElementById("targetRole").value.trim());

    submit.disabled = true;
    status.textContent = "Reviewing CV...";

    try {
        const response = await fetch("/api/resumes/review", {
            method: "POST",
            body: formData
        });

        if (!response.ok) {
            throw new Error(await response.text() || "Review failed.");
        }

        const data = await response.json();
        renderGeneralReview(data);
        status.textContent = "Review ready.";
    } catch (error) {
        status.textContent = error.message;
    } finally {
        submit.disabled = false;
    }
}

async function handleJobApplication(event) {
    event.preventDefault();
    const submit = document.getElementById("application-submit");
    const status = document.getElementById("application-status");
    const fileInput = document.getElementById("application-file");

    if (!fileInput.files.length) {
        status.textContent = "Choose a resume file first.";
        return;
    }

    if (!applicationJob.value) {
        status.textContent = "Select a job first.";
        return;
    }

    const formData = new FormData();
    formData.append("file", fileInput.files[0]);
    formData.append("jobId", applicationJob.value);
    formData.append("candidateName", document.getElementById("application-name").value.trim());
    formData.append("candidateEmail", document.getElementById("application-email").value.trim());

    submit.disabled = true;
    status.textContent = "Analyzing resume for selected job...";

    try {
        const response = await fetch("/api/resumes/upload", {
            method: "POST",
            body: formData
        });

        if (!response.ok) {
            throw new Error(await response.text() || "Job analysis failed.");
        }

        const data = await response.json();
        renderJobAnalysis(data);
        status.textContent = "Job match analysis ready.";
        await loadAnalyses();
    } catch (error) {
        status.textContent = error.message;
    } finally {
        submit.disabled = false;
    }
}

function renderGeneralReview(data) {
    document.getElementById("result-score").textContent = Math.round(data.overallScore ?? 0);
    document.getElementById("result-summary").textContent = data.summary || "No summary returned.";
    document.getElementById("preview-text").textContent = data.extractedTextPreview || "No preview available.";

    renderList(document.getElementById("strengths-list"), data.strengths, "No strengths returned.");
    renderList(document.getElementById("improvements-list"), data.improvements, "No improvement suggestions returned.");
    renderList(document.getElementById("missing-skills-list"), data.missingSkills, "No missing skills identified.");

    resultsSection.classList.remove("hidden");
    resultsSection.scrollIntoView({ behavior: "smooth", block: "start" });
}

function renderJobAnalysis(data) {
    document.getElementById("result-score").textContent = Math.round(data.matchScore ?? 0);
    document.getElementById("result-summary").textContent = data.remarks || "No remarks returned.";
    document.getElementById("preview-text").textContent = [
        `Analysis ID: ${data.analysisId ?? "N/A"}`,
        `Candidate: ${data.candidate?.name || "Unknown"}`,
        `Email: ${data.candidate?.email || "N/A"}`,
        `Job: ${data.jobPosting?.title || "N/A"}`,
        "",
        "Stored resume text preview:",
        trimText(data.candidate?.resumeText || "", 1200)
    ].join("\n");

    renderList(document.getElementById("strengths-list"), [], "Strengths are only returned in general review mode.");
    renderList(document.getElementById("improvements-list"), [data.remarks || "No improvement feedback returned."], "No remarks returned.");
    renderList(document.getElementById("missing-skills-list"), parseMissingSkills(data.missingSkills), "No missing skills identified.");

    resultsSection.classList.remove("hidden");
    resultsSection.scrollIntoView({ behavior: "smooth", block: "start" });
}

function renderList(container, items, emptyText) {
    container.innerHTML = "";
    const values = Array.isArray(items) && items.length ? items : [emptyText];

    values.forEach((item) => {
        const li = document.createElement("li");
        li.textContent = item;
        container.appendChild(li);
    });
}

function parseMissingSkills(rawValue) {
    if (!rawValue) {
        return [];
    }

    try {
        const parsed = JSON.parse(rawValue);
        return Array.isArray(parsed) ? parsed : [String(rawValue)];
    } catch (error) {
        return [String(rawValue)];
    }
}

function splitSkills(value) {
    if (!value) {
        return [];
    }

    return value.split(",").map((item) => item.trim()).filter(Boolean);
}

function trimText(value, length) {
    return value.length > length ? `${value.slice(0, length)}...` : value;
}

function formatDate(value) {
    if (!value) {
        return "Recently added";
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return "Recently added";
    }

    return date.toLocaleDateString(undefined, {
        year: "numeric",
        month: "short",
        day: "numeric"
    });
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

function updateAuthUI() {
    const isAuthenticated = Boolean(state.auth.authenticated);
    const isAdmin = state.auth.role === "ADMIN";

    authBadge.textContent = isAuthenticated
        ? `${state.auth.name || state.auth.email} (${state.auth.role})`
        : "Guest";

    authSummary.textContent = isAuthenticated
        ? `Logged in as ${state.auth.email} with role ${state.auth.role}.`
        : "Not logged in.";

    logoutButton.classList.toggle("hidden", !isAuthenticated);
    jobAuthHint.textContent = isAdmin
        ? "You are logged in as admin and can publish jobs."
        : "Login as an admin to publish jobs.";

    Array.from(jobForm.elements).forEach((element) => {
        if (element.id === "job-status") {
            return;
        }
        element.disabled = !isAdmin;
    });
}
