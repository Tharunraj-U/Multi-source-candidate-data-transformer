package com.eightfold.candidate.service.parser;

import com.eightfold.candidate.exception.SourceParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkedInSessionManager {

    private static final String SESSION_KEY = "linkedin:session";
    private static final Duration SESSION_TTL = Duration.ofHours(12);

    private final StringRedisTemplate redisTemplate;
    private final RestClient restClient = RestClient.create();

    @Value("${linkedin.email:}")
    private String email;

    @Value("${linkedin.password:}")
    private String password;

    public String getSessionCookie() {
        String cached = redisTemplate.opsForValue().get(SESSION_KEY);
        if (cached != null && !cached.isBlank()) {
            return cached;
        }
        return refreshSession();
    }

    public String refreshSession() {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new SourceParseException(
                    "LINKEDIN_CREDENTIALS_MISSING",
                    "LinkedIn credentials are not configured (LINKEDIN_EMAIL / LINKEDIN_PASSWORD)");
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("session_key", email);
            form.add("session_password", password);

            ResponseEntity<Void> response = restClient.post()
                    .uri("https://www.linkedin.com/uas/login-submit")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();

            List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (cookies == null || cookies.isEmpty()) {
                throw new SourceParseException("LINKEDIN_AUTH_FAILED", "LinkedIn login returned no session cookies");
            }

            String cookieHeader = String.join("; ", cookies.stream()
                    .map(c -> c.split(";", 2)[0])
                    .toList());

            redisTemplate.opsForValue().set(SESSION_KEY, cookieHeader, SESSION_TTL);
            log.info("LinkedIn session refreshed");
            return cookieHeader;
        } catch (SourceParseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SourceParseException("LINKEDIN_AUTH_FAILED", "LinkedIn login failed: " + ex.getMessage(), ex);
        }
    }

    public void invalidateSession() {
        redisTemplate.delete(SESSION_KEY);
    }
}
