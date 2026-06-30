package com.eightfold.candidate.service.parser;

import com.eightfold.candidate.domain.entity.RawSource;
import com.eightfold.candidate.domain.enums.LinkType;
import com.eightfold.candidate.domain.enums.SourceType;
import com.eightfold.candidate.domain.model.*;
import com.eightfold.candidate.exception.SourceParseException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class GitHubService implements SourceParser {

    private static final Pattern USERNAME = Pattern.compile(
            "github\\.com/([a-zA-Z0-9](?:[a-zA-Z0-9]|-(?=[a-zA-Z0-9])){0,38})", Pattern.CASE_INSENSITIVE);

    private final RestClient restClient;
    private final String apiToken;

    public GitHubService(@Value("${github.api-token:}") String apiToken) {
        this.apiToken = apiToken;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    @Override
    public boolean supports(SourceType sourceType) {
        return sourceType == SourceType.GITHUB;
    }

    @Override
    public ParsedCandidateDTO parse(RawSource source) {
        String profileUrl = source.getSourceUrl();
        if (profileUrl == null || profileUrl.isBlank()) {
            throw new SourceParseException("GITHUB_URL_MISSING", "GitHub profile URL is required");
        }

        String username = extractUsername(profileUrl);
        JsonNode user = fetchUser(username);
        JsonNode repos = fetchRepos(username);

        return mapFromApi(user, repos, profileUrl, source.getSourceId());
    }

    private String extractUsername(String url) {
        Matcher matcher = USERNAME.matcher(url);
        if (!matcher.find()) {
            throw new SourceParseException("GITHUB_URL_INVALID", "Invalid GitHub profile URL: " + url);
        }
        String username = matcher.group(1);
        if ("settings".equalsIgnoreCase(username) || "orgs".equalsIgnoreCase(username)) {
            throw new SourceParseException("GITHUB_URL_INVALID", "URL does not point to a user profile: " + url);
        }
        return username;
    }

    private JsonNode fetchUser(String username) {
        try {
            return restClient.get()
                    .uri("/users/{username}", username)
                    .headers(headers -> applyAuth(headers))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception ex) {
            throw new SourceParseException("GITHUB_FETCH_FAILED", "Failed to fetch GitHub user: " + ex.getMessage(), ex);
        }
    }

    private JsonNode fetchRepos(String username) {
        try {
            return restClient.get()
                    .uri("/users/{username}/repos?per_page=30&sort=updated", username)
                    .headers(headers -> applyAuth(headers))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception ex) {
            log.warn("Failed to fetch repos for {}: {}", username, ex.getMessage());
            return null;
        }
    }

    private void applyAuth(HttpHeaders headers) {
        if (apiToken != null && !apiToken.isBlank()) {
            headers.setBearerAuth(apiToken);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    private ParsedCandidateDTO mapFromApi(
            JsonNode user, JsonNode repos, String profileUrl, java.util.UUID sourceId) {
        if (user == null || user.isMissingNode()) {
            throw new SourceParseException("GITHUB_NOT_FOUND", "GitHub user not found");
        }

        String name = text(user, "name");
        String login = text(user, "login");
        String bio = text(user, "bio");
        String location = text(user, "location");
        String avatar = text(user, "avatar_url");
        String website = text(user, "blog");

        List<ParsedSkillDTO> skills = extractLanguages(repos);
        List<ParsedLinkDTO> links = new ArrayList<>();
        links.add(ParsedLinkDTO.builder().type(LinkType.GITHUB).url(profileUrl).build());
        if (website != null && !website.isBlank()) {
            links.add(ParsedLinkDTO.builder().type(LinkType.PORTFOLIO).url(website).build());
        }

        String headline = bio;
        if (headline == null && user.has("public_repos")) {
            headline = user.get("public_repos").asInt() + " public repositories";
        }

        return ParsedCandidateDTO.builder()
                .sourceType(SourceType.GITHUB)
                .sourceId(sourceId)
                .fullName(name != null ? name : login)
                .headline(headline)
                .about(bio)
                .location(location)
                .profilePictureUrl(avatar)
                .skills(skills)
                .links(links)
                .build();
    }

    private List<ParsedSkillDTO> extractLanguages(JsonNode repos) {
        List<ParsedSkillDTO> skills = new ArrayList<>();
        if (repos == null || !repos.isArray()) return skills;

        repos.forEach(repo -> {
            String language = text(repo, "language");
            if (language != null && skills.stream().noneMatch(s -> language.equals(s.getName()))) {
                skills.add(ParsedSkillDTO.builder().name(language).build());
            }
        });
        return skills;
    }

    private String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) return null;
        String value = node.get(field).asText();
        return value.isBlank() ? null : value;
    }
}
