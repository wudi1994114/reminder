package com.core.reminder.service;

import com.core.reminder.config.WechatConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Service
@Slf4j
public class StorageService {

    @Autowired
    private WechatAccessTokenService wechatAccessTokenService;

    @Autowired
    private WechatConfig wechatConfig;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String UPLOAD_FILE_META_URL = "https://api.weixin.qq.com/tcb/uploadfile?access_token=%s";

    public String store(MultipartFile file) throws IOException {
        String accessToken = wechatAccessTokenService.getAccessToken();
        
        // 1. 获取上传元数据
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String cloudPath = "mp_avatar/" + UUID.randomUUID().toString() + "." + extension;

        RestTemplate restTemplate = new RestTemplate();
        String getMetaUrl = String.format(UPLOAD_FILE_META_URL, accessToken);
        
        HttpHeaders metaHeaders = new HttpHeaders();
        metaHeaders.setContentType(MediaType.APPLICATION_JSON);
        
        String requestBody = String.format("{\"env\": \"%s\", \"path\": \"%s\"}", wechatConfig.getCloudEnv(), cloudPath);
        HttpEntity<String> metaEntity = new HttpEntity<>(requestBody, metaHeaders);
        
        ResponseEntity<String> metaResponse = restTemplate.postForEntity(getMetaUrl, metaEntity, String.class);
        JsonNode metaRoot = objectMapper.readTree(metaResponse.getBody());

        if (metaRoot.has("errcode") && metaRoot.get("errcode").asInt() != 0) {
            throw new RuntimeException("Failed to get upload metadata: " + metaRoot.get("errmsg").asText());
        }

        String uploadUrl = metaRoot.get("url").asText();
        String token = metaRoot.get("token").asText();
        String authorization = metaRoot.get("authorization").asText();
        String fileId = metaRoot.get("file_id").asText();
        String cosFileId = metaRoot.get("cos_file_id").asText();

        // 2. 上传文件
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("key", cloudPath);
        body.add("Signature", authorization);
        body.add("x-cos-security-token", token);
        body.add("x-cos-meta-fileid", cosFileId);
        
        ByteArrayResource fileAsResource = new ByteArrayResource(file.getBytes()){
            @Override
            public String getFilename(){
                return originalFilename;
            }
        };
        body.add("file", fileAsResource);

        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        HttpEntity<MultiValueMap<String, Object>> uploadEntity = new HttpEntity<>(body, uploadHeaders);
        
        ResponseEntity<String> uploadResponse = restTemplate.postForEntity(uploadUrl, uploadEntity, String.class);

        if (uploadResponse.getStatusCode().is2xxSuccessful()) {
            log.info("File uploaded successfully to WeChat Cloud Storage. FileID: {}", fileId);
            return fileId;
        } else {
            throw new RuntimeException("Failed to upload file to WeChat Cloud Storage. Status: " + uploadResponse.getStatusCode());
        }
    }
} 