package com.eightfold.candidate.service.extraction;

import com.eightfold.candidate.exception.SourceParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Step 1: PDF/DOCX → plain text via Docling Serve (Docker) or Apache Tika fallback.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeTextExtractionService {

    private final Tika tika = new Tika();
    private final DoclingServeClient doclingServeClient;

    @Value("${resume.text.provider:tika}")
    private String textProvider;

    public String extractFromPath(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            throw new SourceParseException("RESUME_NOT_FOUND", "Resume file not found: " + filePath);
        }

        if ("docling".equalsIgnoreCase(textProvider) && doclingServeClient.isEnabled()) {
            try {
                String text = doclingServeClient.convertToText(filePath);
                log.info("Resume text extracted via Docling: {} chars", text.length());
                return text;
            } catch (Exception ex) {
                log.warn("Docling extraction failed, falling back to Tika: {}", ex.getMessage());
            }
        }

        try (InputStream input = Files.newInputStream(filePath)) {
            String text = extractFromStream(input);
            log.debug("Resume text extracted via Tika: {} chars", text.length());
            return text;
        } catch (SourceParseException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new SourceParseException("RESUME_IO_ERROR", "Failed to read resume file", ex);
        }
    }

    public String extractFromStream(InputStream input) {
        try {
            String text = tika.parseToString(input);
            if (text == null || text.isBlank()) {
                throw new SourceParseException("RESUME_EMPTY", "No text could be extracted from resume");
            }
            return text.replace("\r", "").trim();
        } catch (SourceParseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SourceParseException("RESUME_CORRUPT", "Failed to parse resume: " + ex.getMessage(), ex);
        }
    }
}
