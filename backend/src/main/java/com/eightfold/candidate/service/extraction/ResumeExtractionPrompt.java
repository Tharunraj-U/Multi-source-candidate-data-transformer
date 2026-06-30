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
              "experience": [{
                "company": "employer name only",
                "title": "job title only",
                "startDate": "YYYY-MM or YYYY",
                "endDate": "YYYY-MM or YYYY or null",
                "current": false,
                "description": "bullet achievements here"
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
            - Put bullet points and project descriptions in experience.description, NEVER in company/title.
            - institution must be a school/college/university name — not a project or sentence.
            - Split "College Name, City 2018-2022" into institution, startDate=2018, endDate=2022.
            - Do not use section headers (WORK EXPERIENCE, EDUCATION, SKILLS) as field values.
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
