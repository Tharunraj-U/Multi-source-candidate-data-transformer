package com.eightfold.candidate.service.extraction;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ResumeTextExtractionServiceTest {

    private final ResumeTextExtractionService service =
            new ResumeTextExtractionService(new DoclingServeClient("http://localhost:5001", false));

    @Test
    void extractsPlainTextFromTxtStream() throws Exception {
        String sample = "Thangaraj M\nSoftware Development Engineer II\nEDUCATION";
        InputStream input = new ByteArrayInputStream(sample.getBytes(StandardCharsets.UTF_8));

        String text = service.extractFromStream(input);

        assertTrue(text.contains("Thangaraj M"));
        assertTrue(text.contains("EDUCATION"));
        assertFalse(text.contains("\r"));
    }

    @Test
    void throwsOnEmptyStream() {
        InputStream input = new ByteArrayInputStream(new byte[0]);

        assertThrows(Exception.class, () -> service.extractFromStream(input));
    }
}
