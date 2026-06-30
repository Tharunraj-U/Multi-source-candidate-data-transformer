package com.eightfold.candidate.service.extraction;

/**
 * Step 2 of resume pipeline: build the LLM prompt with resume plain text.
 */
public final class ResumeExtractionPrompt {

    private static final String INSTRUCTIONS = """
            You are a resume parser. Read the resume text below and return ONLY valid JSON matching this schema:
            {
              "fullName": "string",
              "headline": "string",
              "location": "string",
              "yearsExperience": number,
              "emails": ["string"],
              "phones": ["string"],
              "skills": ["string"],
              "links": [{ "type": "LINKEDIN|GITHUB|PORTFOLIO|OTHER", "url": "string" }],
              "experience": [{
                "company": "employer or organization name only",
                "title": "job title only",
                "startDate": "YYYY-MM or YYYY",
                "endDate": "YYYY-MM or YYYY or null",
                "current": false,
                "description": "bullet achievements for this role"
              }],
              "education": [{
                "institution": "school or college name only (no city, no years)",
                "degree": "degree name e.g. B.Tech, Bachelor of Engineering",
                "fieldOfStudy": "major or branch",
                "startDate": "YYYY",
                "endDate": "YYYY"
              }]
            }

            Rules:
            EMAILS and LINKS (critical):
            - emails[] must contain ONLY real email addresses with exactly one @ (e.g. name@gmail.com).
            - NEVER put URLs in emails[] — no linkedin.com, github.com, vercel.app, portfolio sites, or domains without @.
            - Put LinkedIn URLs in links with type LINKEDIN; GitHub → GITHUB; personal site/portfolio → PORTFOLIO; other → OTHER.
            - URLs must include https:// when possible.

            EXPERIENCE vs PROJECTS (critical):
            - experience[] is ONLY paid employment or internships at real organizations (Fleet Studio, Null Class, Infosys, Amazon, etc.).
            - company = employer name only (e.g. "Fleet Studio", "Null Class") — NOT a project name.
            - title = job title only (e.g. "Software Developer Intern", "Java Full Stack Developer Intern").
            - Personal projects, capstones, and portfolio builds (e.g. "URL Shortener", "Hospital Management System", "Profile Extraction System") are NOT experience rows.
            - Put project details in the description of the related internship when they belong to that role, or omit them from experience[].
            - Do not use section headers (WORK EXPERIENCE, PROJECTS, EDUCATION) as field values.

            SKILLS (critical):
            - skills[] must be a flat list of individual technologies/tools only (e.g. "Java", "Spring Boot", "React", "MySQL", "Docker").
            - ONE skill per array element — never category headers or grouped lists.
            - WRONG: "Databases MySQL, MongoDB, Redis" or "Security, OWASP Top 10, JWT" or "DevOps & Tools, Docker, Git".
            - RIGHT: separate entries: "MySQL", "MongoDB", "Redis", "OWASP Top 10", "JWT", "Docker", "Git".
            - Do not include soft-skill sentences, architecture descriptions, or comma-separated skill groups.

            OTHER:
            - Put bullet points in experience.description, NEVER in company/title.
            - institution must be a school/college/university — not a project or sentence.
            - Split "College Name, City 2018-2022" into institution, startDate=2018, endDate=2022.
            - yearsExperience = total years of professional work; for students/interns use completed internship months/years only (typically 0-2, not inflated).
            - Use null for missing scalar fields; use [] for missing arrays.

            """;

    private ResumeExtractionPrompt() {}

    public static String build(String resumeText) {
        return INSTRUCTIONS + "--- RESUME TEXT ---\n" + resumeText;
    }

    public static String truncate(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace("\r", "").trim();
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        int head = (int) (maxChars * 0.65);
        int tail = maxChars - head - 20;
        return normalized.substring(0, head) + "\n...[truncated]...\n"
                + normalized.substring(normalized.length() - tail);
    }
}
