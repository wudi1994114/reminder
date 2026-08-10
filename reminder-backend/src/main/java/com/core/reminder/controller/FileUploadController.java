package com.core.reminder.controller;

import com.core.reminder.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
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
            String fileId = storageService.store(file);
            // 注意：我们现在返回的是fileID，前端的逻辑需要能够处理它
            // 前端已经有处理 cloud:// 链接的逻辑，所以这里直接返回fileID是可行的
            return ResponseEntity.ok(Collections.singletonMap("url", fileId));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "File upload failed: " + e.getMessage()));
        }
    }
} 