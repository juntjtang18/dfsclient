package com.fdu.msacs.dfsclient.dfsclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class FileClientService {
    private static final Logger logger = LoggerFactory.getLogger(FileClientService.class);

    @Value("${filesystem.server.url}")
    private String fileSystemServerUrl;
    @Value("${download.dir}")
    private String downloadRoot; // Will be injected from application.properties
    @Autowired
    private RestTemplate restTemplate;

    public String uploadFileToServer(MultipartFile file) {
        String url = fileSystemServerUrl + "/dfs/upload";

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

            logger.info("uploadFile {} ToServer - url: {}", fileResource.getFilename(), url);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            return response.getBody();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("File upload failed: " + e.getMessage());
        }
    }

    public List<String> getFileListFromServer() {
        String url = fileSystemServerUrl + "/dfs/file-list"; // Adjust according to your server API
        ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, null, List.class);
        return response.getBody();
    }
    
    public void downloadFileFromServer(String fileName) {
        String url = "http://localhost:8080/dfs/getfile/" + fileName;
        RestTemplate restTemplate = new RestTemplate();

        try {
            // Fetch the response as a byte array
            ResponseEntity<byte[]> responseEntity = restTemplate.getForEntity(url, byte[].class);

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                // Path to save the file
                File downloadDir = new File(downloadRoot);
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs(); // Create the directory if it doesn't exist
                }

                File outputFile = new File(downloadDir, fileName);
                
                // Save byte[] to file
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(responseEntity.getBody());
                    fos.flush();
                    System.out.println("File saved successfully: " + outputFile.getAbsolutePath());
                } catch (IOException e) {
                    System.err.println("Error writing file: " + e.getMessage());
                }
            } else {
                System.err.println("Failed to download the file. HTTP Status: " + responseEntity.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("Error fetching file from server: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
