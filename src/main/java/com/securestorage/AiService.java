package com.securestorage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    @Value("${GROQ_API_KEY:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    /**
     * Generate smart tags for a file during upload (before encryption).
     * Returns a comma-separated string of tags, or empty string on failure.
     */
    public String generateTags(byte[] fileContent, String contentType, String filename) {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("Groq API key not configured, skipping AI tagging");
            return "";
        }

        try {
            if (contentType != null && contentType.startsWith("image/")) {
                // Groq text models can't analyze images directly, use filename-based tags
                return generateFilenameOnlyTags(filename, contentType);
            } else if (isTextBasedFile(contentType, filename)) {
                String textContent = new String(fileContent);
                if (textContent.length() > 4000) {
                    textContent = textContent.substring(0, 4000);
                }
                return generateTextTags(textContent, filename);
            } else {
                return generateFilenameOnlyTags(filename, contentType);
            }
        } catch (Exception e) {
            logger.error("AI tagging failed for file '{}': {}", filename, e.getMessage());
            return "";
        }
    }

    /**
     * Generate a summary of a file on demand (after decryption).
     * Returns a 2-3 sentence summary string, or error message on failure.
     */
    public String generateSummary(byte[] fileContent, String contentType, String filename) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "AI summary unavailable: API key not configured.";
        }

        try {
            if (contentType != null && contentType.startsWith("image/")) {
                return "Image summary: This is an image file named '" + filename + "'. Upload a text-based document to get a detailed AI summary.";
            } else if (isTextBasedFile(contentType, filename)) {
                String textContent = new String(fileContent);
                if (textContent.length() > 8000) {
                    textContent = textContent.substring(0, 8000);
                }
                return generateTextSummary(textContent, filename);
            } else {
                return "Summary not available for this file type (" + contentType + ").";
            }
        } catch (Exception e) {
            logger.error("AI summary failed for file '{}': {}", filename, e.getMessage());
            return "Failed to generate summary: " + e.getMessage();
        }
    }

    // ========== PRIVATE METHODS ==========

    private String generateTextTags(String textContent, String filename) {
        String prompt = "Analyze the following document (filename: " + filename + ") and provide exactly 3-5 short descriptive tags " +
                "that categorize its content. Return ONLY the tags as a comma-separated list, nothing else.\n\n" +
                "Document content:\n" + textContent;
        return callGroqApi(prompt);
    }

    private String generateTextSummary(String textContent, String filename) {
        String prompt = "Summarize the following document (filename: " + filename + ") in 2-3 clear, concise sentences. " +
                "Focus on the main topic and key points.\n\n" +
                "Document content:\n" + textContent;
        return callGroqApi(prompt);
    }

    private String generateFilenameOnlyTags(String filename, String contentType) {
        String prompt = "Based on this filename and content type, provide 2-3 short descriptive tags. " +
                "Return ONLY the tags as a comma-separated list, nothing else.\n" +
                "Filename: " + filename + "\nContent-Type: " + contentType;
        return callGroqApi(prompt);
    }

    @SuppressWarnings("unchecked")
    private String callGroqApi(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = Map.of(
            "model", MODEL,
            "messages", List.of(
                Map.of("role", "system", "content", "You are a helpful file analysis assistant. Be concise and precise."),
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.3,
            "max_tokens", 200
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_URL, entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null) {
                    return ((String) message.get("content")).trim();
                }
            }
        }

        return "";
    }

    private boolean isTextBasedFile(String contentType, String filename) {
        if (contentType != null) {
            if (contentType.startsWith("text/")) return true;
            if (contentType.equals("application/pdf")) return true;
            if (contentType.contains("json") || contentType.contains("xml")) return true;
            if (contentType.contains("javascript") || contentType.contains("html")) return true;
        }
        if (filename != null) {
            String lower = filename.toLowerCase();
            return lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".csv") ||
                   lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".html") ||
                   lower.endsWith(".java") || lower.endsWith(".py") || lower.endsWith(".js") ||
                   lower.endsWith(".log") || lower.endsWith(".yaml") || lower.endsWith(".yml");
        }
        return false;
    }
}
