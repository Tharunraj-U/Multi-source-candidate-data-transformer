package com.eightfold.candidate.service.extraction;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DoclingServeClientTest {

    @Test
    void extractTextPrefersTextContent() throws Exception {
        DoclingServeClient client = new DoclingServeClient("http://localhost:5001", true);
        ObjectMapper mapper = new ObjectMapper();
        String json = """
                {
                  "status": "success",
                  "document": {
                    "text_content": "Thangaraj M\\nSoftware Development Engineer II",
                    "md_content": "# Thangaraj M"
                  }
                }
                """;

        String text = client.extractText(mapper.readTree(json));

        assertTrue(text.contains("Thangaraj M"));
        assertTrue(text.contains("Software Development Engineer II"));
    }

    @Test
    void extractTextFallsBackToMarkdown() throws Exception {
        DoclingServeClient client = new DoclingServeClient("http://localhost:5001", true);
        ObjectMapper mapper = new ObjectMapper();
        String json = """
                {
                  "status": "success",
                  "document": {
                    "text_content": "",
                    "md_content": "## Education\\nSri Eshwar College of Engineering"
                  }
                }
                """;

        String text = client.extractText(mapper.readTree(json));

        assertTrue(text.contains("Sri Eshwar College of Engineering"));
    }
}
