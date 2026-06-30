package com.eightfold.candidate.service.parser;

import com.eightfold.candidate.domain.enums.LinkType;
import com.eightfold.candidate.domain.model.ParsedLinkDTO;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class ResumeContactSanitizer {

    private static final Pattern EMAIL = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern URL_LIKE = Pattern.compile(
            "(?i)^(https?://|www\\.|linkedin\\.com|github\\.com|.*\\.(app|dev|io|vercel\\.app)(/|$))");

    private ResumeContactSanitizer() {}

    public static List<String> filterEmails(List<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return List.of();
        }
        Set<String> kept = new LinkedHashSet<>();
        for (String email : emails) {
            if (email == null || email.isBlank()) {
                continue;
            }
            String trimmed = email.trim().toLowerCase(Locale.ROOT);
            if (EMAIL.matcher(trimmed).matches()) {
                kept.add(trimmed);
            }
        }
        return List.copyOf(kept);
    }

    public static List<ParsedLinkDTO> linksFromMisplacedContacts(List<String> emails, List<String> urls) {
        List<ParsedLinkDTO> links = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        if (emails != null) {
            for (String value : emails) {
                if (value != null && looksLikeUrl(value)) {
                    addLink(links, seen, value);
                }
            }
        }
        if (urls != null) {
            for (String value : urls) {
                if (value != null && looksLikeUrl(value)) {
                    addLink(links, seen, value);
                }
            }
        }
        return links;
    }

    public static boolean looksLikeUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.contains("@")) {
            return false;
        }
        return URL_LIKE.matcher(trimmed).find()
                || trimmed.contains("linkedin.com")
                || trimmed.contains("github.com")
                || trimmed.matches("(?i)[a-z0-9-]+\\.(app|dev|io|vercel\\.app)(/.*)?");
    }

    public static LinkType inferLinkType(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains("linkedin.com")) {
            return LinkType.LINKEDIN;
        }
        if (lower.contains("github.com")) {
            return LinkType.GITHUB;
        }
        if (lower.contains("vercel.app") || lower.contains("portfolio") || lower.matches(".*\\.(dev|io|app)(/.*)?$")) {
            return LinkType.PORTFOLIO;
        }
        return LinkType.OTHER;
    }

    private static void addLink(List<ParsedLinkDTO> links, Set<String> seen, String raw) {
        String url = normalizeUrl(raw.trim());
        if (seen.add(url.toLowerCase(Locale.ROOT))) {
            links.add(ParsedLinkDTO.builder()
                    .type(inferLinkType(url))
                    .url(url)
                    .build());
        }
    }

    private static String normalizeUrl(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        return "https://" + url;
    }
}
