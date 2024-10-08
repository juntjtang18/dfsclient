package com.fdu.msacs.dfsclient.dfsclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;

@Service
public class FileClientService {
    private static final Logger logger = LoggerFactory.getLogger(FileClientService.class);

    @Value("${filesystem.server.url}")
    private String fileSystemServerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String uploadFileToServer(MultipartFile file) {
        String url = fileSystemServerUrl + "/files/upload";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        try {
            // Wrap the file as a ByteArrayResource
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename(); // Get the original filename
                }
            };
            body.add("file", fileResource);

            // Set the headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            logger.info("uploadFileToServer - url: {}", url);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            return response.getBody();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("File upload failed: " + e.getMessage());
        }
    }

    public byte[] downloadFileFromServer(String filename) {
        String url = fileSystemServerUrl + "/files/download/" + filename;
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
        return response.getBody();
    }
}
