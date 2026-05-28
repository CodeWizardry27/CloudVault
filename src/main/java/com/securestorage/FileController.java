package com.securestorage;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal Jwt principal) {
        try {
            fileService.uploadFile(file, principal.getSubject());
            return ResponseEntity.ok("Upload successful!");
        } catch (IllegalStateException | IllegalArgumentException e) {
            // Validation errors - return 400
            return ResponseEntity.badRequest().body("Validation error: " + e.getMessage());
        } catch (RuntimeException e) {
            // Business logic errors (e.g., storage limit, AWS errors)
            if (e.getMessage().contains("Storage Limit Exceeded")) {
                return ResponseEntity.status(413).body(e.getMessage());
            }
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        } catch (Exception e) {
            // Unexpected errors
            return ResponseEntity.status(500).body("Unexpected error during upload: " + e.getMessage());
        }
    }

    @GetMapping
    public List<FileEntity> list(@AuthenticationPrincipal Jwt principal) {
        // Pass the logged-in user's ID to the service
        return fileService.listFiles(principal.getSubject());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable String id) throws Exception {
        // 1. Get the decrypted data
        byte[] data = fileService.downloadFile(id);

        // 2. Get the file details (so we know the name)
        FileEntity entity = fileService.getFileMetadata(id);

        // 3. Return the file with the correct name
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + entity.getFilename() + "\"")
                .body(data);
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