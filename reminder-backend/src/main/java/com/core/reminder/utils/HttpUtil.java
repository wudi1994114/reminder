package com.core.reminder.utils;

import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class HttpUtil {
    private static final RestTemplate restTemplate = new RestTemplate();

    /**
     * 发送GET请求
     * @param url 请求URL
     * @param responseType 响应类型
     * @return 响应结果
     */
    public static <T> T get(String url, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36");
        headers.set("Accept", "application/json");
        headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.set("Origin", "https://timor.tech");
        headers.set("Referer", "https://timor.tech/");
        
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        ResponseEntity<T> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            responseType
        );
        
        return response.getBody();
    }

    /**
     * 发送POST请求
     * @param url 请求URL
     * @param body 请求体
     * @param responseType 响应类型
     * @return 响应结果
     */
    public static <T> T post(String url, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<?> entity = new HttpEntity<>(body, headers);
        
        ResponseEntity<T> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            responseType
        );
        
        return response.getBody();
    }

    /**
     * 发送带表单数据的POST请求
     * @param url 请求URL
     * @param formData 表单数据
     * @param responseType 响应类型
     * @return 响应结果
     */
    public static <T> T postForm(String url, MultiValueMap<String, String> formData, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(formData, headers);
        
        ResponseEntity<T> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            responseType
        );
        
        return response.getBody();
    }
} 