package com.securestorage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    @Value("${GEMINI_API_KEY:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    /**
     * Generate smart tags for a file during upload (before encryption).
     * Returns a comma-separated string of tags, or empty string on failure.
     */
    public String generateTags(byte[] fileContent, String contentType, String filename) {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("Gemini API key not configured, skipping AI tagging");
            return "";
        }

        try {
            if (contentType != null && contentType.startsWith("image/")) {
                return generateImageTags(fileContent, contentType);
            } else if (isTextBasedFile(contentType, filename)) {
                String textContent = new String(fileContent);
                // Limit text to 4000 chars to stay within API limits
                if (textContent.length() > 4000) {
                    textContent = textContent.substring(0, 4000);
                }
                return generateTextTags(textContent, filename);
            } else {
                // For unsupported file types, generate tags based on filename only
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
                return generateImageSummary(fileContent, contentType);
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

    private String generateImageTags(byte[] imageBytes, String contentType) {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(Map.of(
                "parts", List.of(
                    Map.of(
                        "inlineData", Map.of(
                            "mimeType", contentType,
                            "data", base64Image
                        )
                    ),
                    Map.of("text", "Analyze this image and provide exactly 3-5 short descriptive tags that categorize it. " +
                            "Return ONLY the tags as a comma-separated list, nothing else. " +
                            "Example: landscape, nature, mountains, sunset")
                )
            ))
        );

        return callGeminiApi(requestBody);
    }

    private String generateImageSummary(byte[] imageBytes, String contentType) {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(Map.of(
                "parts", List.of(
                    Map.of(
                        "inlineData", Map.of(
                            "mimeType", contentType,
                            "data", base64Image
                        )
                    ),
                    Map.of("text", "Describe this image in 2-3 clear sentences. Focus on what the image shows, " +
                            "the key subjects, and any notable details.")
                )
            ))
        );

        return callGeminiApi(requestBody);
    }

    private String generateTextTags(String textContent, String filename) {
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(Map.of(
                "parts", List.of(
                    Map.of("text", "Analyze the following document (filename: " + filename + ") and provide exactly 3-5 short descriptive tags " +
                            "that categorize its content. Return ONLY the tags as a comma-separated list, nothing else.\n\n" +
                            "Document content:\n" + textContent)
                )
            ))
        );

        return callGeminiApi(requestBody);
    }

    private String generateTextSummary(String textContent, String filename) {
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(Map.of(
                "parts", List.of(
                    Map.of("text", "Summarize the following document (filename: " + filename + ") in 2-3 clear, concise sentences. " +
                            "Focus on the main topic and key points.\n\n" +
                            "Document content:\n" + textContent)
                )
            ))
        );

        return callGeminiApi(requestBody);
    }

    private String generateFilenameOnlyTags(String filename, String contentType) {
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(Map.of(
                "parts", List.of(
                    Map.of("text", "Based on this filename and content type, provide 2-3 short descriptive tags. " +
                            "Return ONLY the tags as a comma-separated list.\n" +
                            "Filename: " + filename + "\nContent-Type: " + contentType)
                )
            ))
        );

        return callGeminiApi(requestBody);
    }

    @SuppressWarnings("unchecked")
    private String callGeminiApi(Map<String, Object> requestBody) {
        String url = GEMINI_URL + "?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return ((String) parts.get(0).get("text")).trim();
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
