package com.eightfold.candidate.service.parser;

import com.eightfold.candidate.domain.entity.RawSource;
import com.eightfold.candidate.domain.enums.LinkType;
import com.eightfold.candidate.domain.enums.SourceType;
import com.eightfold.candidate.domain.model.*;
import com.eightfold.candidate.exception.SourceParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkedInScraperService implements SourceParser {

    private static final Pattern PROFILE_SLUG = Pattern.compile(
            "linkedin\\.com/in/([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE);

    private final LinkedInSessionManager sessionManager;
    private final RestClient restClient = RestClient.create();

    @Override
    public boolean supports(SourceType sourceType) {
        return sourceType == SourceType.LINKEDIN;
    }

    @Override
    public ParsedCandidateDTO parse(RawSource source) {
        String profileUrl = source.getSourceUrl();
        if (profileUrl == null || profileUrl.isBlank()) {
            throw new SourceParseException("LINKEDIN_URL_MISSING", "LinkedIn profile URL is required");
        }

        String slug = extractSlug(profileUrl);
        String html = fetchProfileHtml(profileUrl);
        return mapFromHtml(html, slug, profileUrl, source.getSourceId());
    }

    private String extractSlug(String url) {
        Matcher matcher = PROFILE_SLUG.matcher(url);
        if (!matcher.find()) {
            throw new SourceParseException("LINKEDIN_URL_INVALID", "Invalid LinkedIn profile URL: " + url);
        }
        return matcher.group(1);
    }

    private String fetchProfileHtml(String profileUrl) {
        try {
            return fetchWithSession(profileUrl);
        } catch (SourceParseException ex) {
            if ("LINKEDIN_AUTH_FAILED".equals(ex.getErrorCode())) {
                sessionManager.invalidateSession();
                return fetchWithSession(profileUrl);
            }
            throw ex;
        }
    }

    private String fetchWithSession(String profileUrl) {
        try {
            String cookie = sessionManager.getSessionCookie();
            String html = restClient.get()
                    .uri(profileUrl)
                    .header(HttpHeaders.COOKIE, cookie)
                    .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; CandidateBot/1.0)")
                    .retrieve()
                    .body(String.class);

            if (html == null || html.isBlank()) {
                throw new SourceParseException("LINKEDIN_FETCH_FAILED", "Empty response from LinkedIn");
            }
            if (html.contains("authwall") || html.contains("login-submit")) {
                throw new SourceParseException("LINKEDIN_AUTH_FAILED", "LinkedIn session expired or blocked");
            }
            return html;
        } catch (SourceParseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SourceParseException("LINKEDIN_FETCH_FAILED", "Failed to fetch LinkedIn profile: " + ex.getMessage(), ex);
        }
    }

    private ParsedCandidateDTO mapFromHtml(String html, String slug, String profileUrl, java.util.UUID sourceId) {
        Document doc = Jsoup.parse(html);

        String fullName = firstText(doc, "h1.text-heading-xlarge", "h1.top-card-layout__title", "title");
        String headline = firstText(doc, ".text-body-medium", ".top-card-layout__headline");
        String location = firstText(doc, ".text-body-small", ".top-card__subline-item");
        String about = sectionText(doc, "about");
        String profileImage = firstAttr(doc, "img.pv-top-card-profile-picture__image", "src");

        List<ParsedExperienceDTO> experience = parseExperience(doc);
        List<ParsedEducationDTO> education = parseEducation(doc);
        List<ParsedSkillDTO> skills = parseSkills(doc);

        List<ParsedLinkDTO> links = new ArrayList<>();
        links.add(ParsedLinkDTO.builder().type(LinkType.LINKEDIN).url(profileUrl).build());

        return ParsedCandidateDTO.builder()
                .sourceType(SourceType.LINKEDIN)
                .sourceId(sourceId)
                .fullName(fullName != null ? fullName : slug)
                .headline(headline)
                .about(about)
                .location(location)
                .profilePictureUrl(profileImage)
                .experience(experience)
                .education(education)
                .skills(skills)
                .links(links)
                .build();
    }

    private String firstText(Document doc, String... selectors) {
        for (String selector : selectors) {
            Element el = doc.selectFirst(selector);
            if (el != null && !el.text().isBlank()) {
                return el.text().trim();
            }
        }
        return null;
    }

    private String firstAttr(Document doc, String selector, String attr) {
        Element el = doc.selectFirst(selector);
        return el != null ? el.attr(attr) : null;
    }

    private String sectionText(Document doc, String sectionId) {
        Element section = doc.selectFirst("#" + sectionId);
        if (section == null) {
            section = doc.selectFirst("[data-section='" + sectionId + "']");
        }
        return section != null ? section.text().trim() : null;
    }

    private List<ParsedExperienceDTO> parseExperience(Document doc) {
        List<ParsedExperienceDTO> results = new ArrayList<>();
        Elements items = doc.select("#experience ~ .pvs-list__outer-container li, section[id*=experience] li");
        for (Element item : items) {
            Elements spans = item.select("span[aria-hidden=true]");
            if (spans.size() >= 2) {
                results.add(ParsedExperienceDTO.builder()
                        .title(spans.get(0).text().trim())
                        .company(spans.get(1).text().trim())
                        .build());
            }
        }
        return results;
    }

    private List<ParsedEducationDTO> parseEducation(Document doc) {
        List<ParsedEducationDTO> results = new ArrayList<>();
        Elements items = doc.select("#education ~ .pvs-list__outer-container li, section[id*=education] li");
        for (Element item : items) {
            Elements spans = item.select("span[aria-hidden=true]");
            if (!spans.isEmpty()) {
                results.add(ParsedEducationDTO.builder()
                        .institution(spans.get(0).text().trim())
                        .degree(spans.size() > 1 ? spans.get(1).text().trim() : null)
                        .build());
            }
        }
        return results;
    }

    private List<ParsedSkillDTO> parseSkills(Document doc) {
        List<ParsedSkillDTO> results = new ArrayList<>();
        Elements items = doc.select("#skills ~ .pvs-list__outer-container li, section[id*=skills] li");
        for (Element item : items) {
            Element skill = item.selectFirst("span[aria-hidden=true]");
            if (skill != null && !skill.text().isBlank()) {
                results.add(ParsedSkillDTO.builder().name(skill.text().trim()).build());
            }
        }
        return results;
    }
}
