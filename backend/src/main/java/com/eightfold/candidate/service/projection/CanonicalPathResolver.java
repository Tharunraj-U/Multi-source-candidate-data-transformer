package com.eightfold.candidate.service.projection;

import com.eightfold.candidate.dto.response.ApiDtos;
import com.eightfold.candidate.exception.ProjectionException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CanonicalPathResolver {

    private static final Pattern INDEXED_ROOT = Pattern.compile("^([a-z_]+)(?:\\[(\\d+)])?$");

    private CanonicalPathResolver() {}

    public static void validatePath(String path) {
        if (path == null || path.isBlank()) {
            throw new ProjectionException(path, "Path must not be blank");
        }
        String[] segments = path.split("\\.");
        Matcher matcher = INDEXED_ROOT.matcher(segments[0]);
        if (!matcher.matches()) {
            throw new ProjectionException(path, "Unknown or invalid canonical path: " + path);
        }
        String root = matcher.group(1);
        if (!isKnownRoot(root)) {
            throw new ProjectionException(path, "Unknown canonical path root: " + root);
        }
        for (int i = 1; i < segments.length; i++) {
            if (!isKnownProperty(segments[i])) {
                throw new ProjectionException(path, "Unknown property: " + segments[i]);
            }
        }
    }

    private static boolean isKnownRoot(String root) {
        return switch (root) {
            case "full_name", "headline", "location", "years_experience", "candidate_id",
                 "overall_confidence", "status", "emails", "phones", "skills",
                 "experience", "education", "links" -> true;
            default -> false;
        };
    }

    private static boolean isKnownProperty(String property) {
        return switch (property) {
            case "address", "validation_status", "validationStatus", "name", "canonical",
                 "company", "title", "description", "start_date", "startDate",
                 "end_date", "endDate", "institution", "degree", "field_of_study",
                 "fieldOfStudy", "url", "type" -> true;
            default -> false;
        };
    }

    public static Object resolve(ApiDtos.CandidateResponseDto profile, String path) {
        return resolvePath(profile, path);
    }

    private static Object resolvePath(ApiDtos.CandidateResponseDto profile, String path) {
        String[] segments = path.split("\\.");
        Object current = profile;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (i == 0) {
                current = resolveRoot(profile, segment, path);
                current = collapseIndexedValue(current, segment);
            } else {
                current = resolveProperty(current, segment, path);
            }
        }
        return current;
    }

    private static Object collapseIndexedValue(Object value, String segment) {
        Matcher matcher = INDEXED_ROOT.matcher(segment);
        if (matcher.matches() && matcher.group(2) != null && value instanceof ApiDtos.EmailDto email) {
            return email.getAddress();
        }
        return value;
    }

    private static Object resolveRoot(ApiDtos.CandidateResponseDto profile, String segment, String fullPath) {
        Matcher matcher = INDEXED_ROOT.matcher(segment);
        if (!matcher.matches()) {
            throw new ProjectionException(fullPath, "Unknown or invalid canonical path: " + fullPath);
        }
        String root = matcher.group(1);
        String index = matcher.group(2);
        return switch (root) {
            case "full_name" -> profile.getFullName();
            case "headline" -> profile.getHeadline();
            case "location" -> profile.getLocation();
            case "years_experience" -> profile.getYearsExperience();
            case "candidate_id" -> profile.getCandidateId();
            case "overall_confidence" -> profile.getOverallConfidence();
            case "status" -> profile.getStatus() != null ? profile.getStatus().name() : null;
            case "emails" -> listAt(profile.getEmails(), index, fullPath);
            case "phones" -> listAt(profile.getPhones(), index, fullPath);
            case "skills" -> listAt(profile.getSkills(), index, fullPath);
            case "experience" -> listAt(profile.getExperience(), index, fullPath);
            case "education" -> listAt(profile.getEducation(), index, fullPath);
            case "links" -> listAt(profile.getLinks(), index, fullPath);
            default -> throw new ProjectionException(fullPath, "Unknown canonical path root: " + root);
        };
    }

    private static Object listAt(List<?> list, String indexStr, String fullPath) {
        if (indexStr == null) {
            return list;
        }
        int index = Integer.parseInt(indexStr);
        if (list == null || index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    private static Object resolveProperty(Object value, String property, String fullPath) {
        if (value == null) {
            return null;
        }
        return switch (property) {
            case "address" -> value instanceof ApiDtos.EmailDto e ? e.getAddress() : propertyError(fullPath, property);
            case "validation_status", "validationStatus" -> value instanceof ApiDtos.EmailDto e
                    ? e.getValidationStatus() : propertyError(fullPath, property);
            case "name" -> value instanceof ApiDtos.SkillDto s ? s.getName() : propertyError(fullPath, property);
            case "canonical" -> value instanceof ApiDtos.SkillDto s ? s.getCanonical() : propertyError(fullPath, property);
            case "company" -> value instanceof ApiDtos.ExperienceDto e ? e.getCompany() : propertyError(fullPath, property);
            case "title" -> value instanceof ApiDtos.ExperienceDto e ? e.getTitle() : propertyError(fullPath, property);
            case "description" -> value instanceof ApiDtos.ExperienceDto e ? e.getDescription() : propertyError(fullPath, property);
            case "start_date", "startDate" -> value instanceof ApiDtos.ExperienceDto e ? e.getStartDate()
                    : value instanceof ApiDtos.EducationDto ed ? ed.getStartDate() : propertyError(fullPath, property);
            case "end_date", "endDate" -> value instanceof ApiDtos.ExperienceDto e ? e.getEndDate()
                    : value instanceof ApiDtos.EducationDto ed ? ed.getEndDate() : propertyError(fullPath, property);
            case "institution" -> value instanceof ApiDtos.EducationDto e ? e.getInstitution() : propertyError(fullPath, property);
            case "degree" -> value instanceof ApiDtos.EducationDto e ? e.getDegree() : propertyError(fullPath, property);
            case "field_of_study", "fieldOfStudy" -> value instanceof ApiDtos.EducationDto e
                    ? e.getFieldOfStudy() : propertyError(fullPath, property);
            case "url" -> value instanceof ApiDtos.LinkDto l ? l.getUrl() : propertyError(fullPath, property);
            case "type" -> value instanceof ApiDtos.LinkDto l ? l.getType() : propertyError(fullPath, property);
            default -> throw new ProjectionException(fullPath, "Unknown property: " + property);
        };
    }

    private static ProjectionException propertyError(String fullPath, String property) {
        return new ProjectionException(fullPath, "Property '" + property + "' is not valid for path " + fullPath);
    }

    public static Map<String, Object> confidenceAsMap(Map<String, BigDecimal> confidence) {
        if (confidence == null) {
            return Map.of();
        }
        Map<String, Object> mapped = new HashMap<>();
        confidence.forEach(mapped::put);
        return mapped;
    }
}
