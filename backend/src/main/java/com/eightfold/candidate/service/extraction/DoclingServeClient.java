package com.eightfold.candidate.service.extraction;

import com.eightfold.candidate.exception.SourceParseException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;

/**
 * Calls Docling Serve (Docker) to convert PDF/DOCX → markdown/text.
 * @see <a href="https://github.com/docling-project/docling-serve">docling-serve</a>
 */
@Slf4j
@Component
public class DoclingServeClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final boolean enabled;

    public DoclingServeClient(
            @Value("${docling.base-url:http://localhost:5001}") String baseUrl,
            @Value("${docling.enabled:false}") boolean enabled) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.enabled = enabled;
        this.restClient = RestClient.builder().baseUrl(this.baseUrl).build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String convertToText(Path filePath) {
        if (!enabled) {
            throw new SourceParseException("DOCLING_DISABLED", "Docling is not enabled");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", new FileSystemResource(filePath.toFile()));
        body.add("to_formats", "text");
        body.add("to_formats", "md");
        body.add("from_formats", "pdf");
        body.add("from_formats", "docx");

        try {
            JsonNode response = restClient.post()
                    .uri("/v1/convert/file")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            return extractText(response);
        } catch (Exception ex) {
            throw new SourceParseException("DOCLING_CONVERT_FAILED",
                    "Docling conversion failed: " + ex.getMessage(), ex);
        }
    }

    String extractText(JsonNode response) {
        if (response == null) {
            throw new SourceParseException("DOCLING_EMPTY_RESPONSE", "Docling returned empty response");
        }
        String status = response.path("status").asText("");
        if ("failure".equalsIgnoreCase(status)) {
            throw new SourceParseException("DOCLING_FAILURE",
                    "Docling status failure: " + response.path("errors"));
        }

        JsonNode document = response.path("document");
        String text = textOrNull(document, "text_content");
        if (text != null && !text.isBlank()) {
            return text.trim();
        }
        String markdown = textOrNull(document, "md_content");
        if (markdown != null && !markdown.isBlank()) {
            return markdown.trim();
        }
        throw new SourceParseException("DOCLING_NO_TEXT", "Docling returned no text_content or md_content");
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) {
            return null;
        }
        return node.path(field).asText();
    }
}
