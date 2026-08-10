package com.core.reminder.controller;

import com.core.reminder.service.StorageService;
import com.core.reminder.service.StoredFile;
import com.core.reminder.service.FileTooLargeException;
import com.core.reminder.service.StorageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final StorageService storageService;

    @Autowired
    public FileUploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> handleFileUpload(@RequestParam("file") MultipartFile file) {
        try {
            StoredFile storedFile = storageService.store(file);
            Map<String, String> response = new LinkedHashMap<>();
            response.put("url", storedFile.getUrl());
            response.put("objectName", storedFile.getObjectName());
            return ResponseEntity.ok(response);
        } catch (FileTooLargeException e) {
            return ResponseEntity.status(413)
                    .body(Collections.singletonMap("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", e.getMessage()));
        } catch (StorageException e) {
            return ResponseEntity.status(502)
                    .body(Collections.singletonMap("error", "File storage service is unavailable"));
        }
    }
}
