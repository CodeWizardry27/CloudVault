package com.securestorage;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final AiService aiService;

    public FileController(FileService fileService, AiService aiService) {
        this.fileService = fileService;
        this.aiService = aiService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal Jwt principal) {
        try {
            fileService.uploadFile(file, principal.getSubject());
            return ResponseEntity.ok("Upload successful!");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Validation error: " + e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Storage Limit Exceeded")) {
                return ResponseEntity.status(413).body(e.getMessage());
            }
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Unexpected error during upload: " + e.getMessage());
        }
    }

    @GetMapping
    public List<FileEntity> list(@AuthenticationPrincipal Jwt principal) {
        return fileService.listFiles(principal.getSubject());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable String id) throws Exception {
        byte[] data = fileService.downloadFile(id);
        FileEntity entity = fileService.getFileMetadata(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + entity.getFilename() + "\"")
                .body(data);
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<Map<String, String>> summarize(@PathVariable String id, @AuthenticationPrincipal Jwt principal) {
        try {
            FileEntity entity = fileService.getFileMetadata(id);

            // Security check: ensure the user owns this file
            if (!entity.getOwnerId().equals(principal.getSubject())) {
                return ResponseEntity.status(403).body(Map.of("error", "Unauthorized access"));
            }

            // Check if summary is already cached (skip if it contains error messages)
            String cachedSummary = entity.getAiSummary();
            if (cachedSummary != null && !cachedSummary.isEmpty()
                    && !cachedSummary.contains("Failed to generate")
                    && !cachedSummary.contains("Too Many Requests")
                    && !cachedSummary.contains("RESOURCE_EXHAUSTED")
                    && !cachedSummary.contains("quota")) {
                return ResponseEntity.ok(Map.of("summary", cachedSummary, "cached", "true"));
            }

            // Decrypt the file and generate summary
            byte[] decryptedContent = fileService.downloadFile(id);
            String summary = aiService.generateSummary(decryptedContent, entity.getContentType(), entity.getFilename());

            // Only cache successful summaries (not error messages)
            if (!summary.contains("Failed to generate") && !summary.contains("unavailable")) {
                entity.setAiSummary(summary);
                fileService.updateFileMetadata(entity);
            }

            return ResponseEntity.ok(Map.of("summary", summary, "cached", "false"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to generate summary: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable String id, @AuthenticationPrincipal Jwt principal) {
        try {
            fileService.deleteFile(id, principal.getSubject());
            return ResponseEntity.ok("File deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting file: " + e.getMessage());
        }
    }
}