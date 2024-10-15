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

import com.fdu.msacs.dfsclient.dfsclient.FileClientController.RequestUpload;
import com.fdu.msacs.dfsclient.dfsclient.FileClientController.UploadResponse;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class FileClientService {
    private static final Logger logger = LoggerFactory.getLogger(FileClientService.class);

    @Value("${download.dir}")
    private String downloadRoot; // Will be injected from application.properties
    @Autowired
    private RestTemplate restTemplate;

    public String uploadFileToServer(MultipartFile file) {
        // Step 1: Get the upload URL from the metadata server
        String uploadUrl = getUploadUrl(file.getOriginalFilename());
        logger.info("Retrieved upload URL from metadata server: {}", uploadUrl);

        // Step 2: Proceed with the file upload if an upload URL was successfully obtained
        if (uploadUrl != null) {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            try {
                // Wrap the file as a ByteArrayResource to preserve the original filename
                ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename();
                    }
                };
                body.add("file", fileResource);

                // Set the headers for the request, specifying that it is a multipart form-data request
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                logger.info("Uploading file '{}' to server - URL: {}", fileResource.getFilename(), uploadUrl);

                // Create an HttpEntity containing the request body and headers
                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                // Send the POST request to the upload URL using restTemplate and get the response
                ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, requestEntity, String.class);

                // Check for a successful response before returning the body
                if (response.getStatusCode().is2xxSuccessful()) {
                    return response.getBody();
                } else {
                    logger.error("File upload failed with status code: {}", response.getStatusCode());
                    throw new RuntimeException("File upload failed with status: " + response.getStatusCode());
                }
            } catch (IOException e) {
                logger.error("File upload failed due to IOException: {}", e.getMessage());
                throw new RuntimeException("File upload failed: " + e.getMessage());
            }
        } else {
            throw new RuntimeException("Failed to retrieve upload URL from the metadata server.");
        }
    }


    private String getUploadUrl(String filename) {
        String metadataUrl = "http://localhost:8080/metadata/upload-url"; // Endpoint to get the upload URL

        // Prepare the request object with the required parameters (e.g., filename)
        RequestUpload requestUpload = new RequestUpload("some-unique-uuid", filename);

        try {
            // Send a POST request with the request body to get the upload URL
            ResponseEntity<UploadResponse> response = restTemplate.postForEntity(
                metadataUrl,
                requestUpload,
                UploadResponse.class
            );

            // Check if the response is successful and has a valid body
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                logger.error("Failed to retrieve upload URL. HTTP Status: {}, Response Body: {}", 
                             response.getStatusCode(), response.getBody());
                return null; // Return null on failure
            }

            UploadResponse uploadResponse = response.getBody();
            String nodeUrl = uploadResponse.getNodeUrl();

            // Log and return the node URL if available
            if (nodeUrl != null) {
                logger.info("Successfully retrieved upload URL: {}", nodeUrl);
                return nodeUrl;
            } else {
                logger.error("Node URL in response is null.");
            }
        } catch (RestClientException e) {
            logger.error("Error while fetching upload URL: {}", e.getMessage());
        }

        return null; // Return null if there's an error or no valid URL
    }


    public List<String> getFileListFromServer() {
        String url = "http://localhost:8080/dfs/file-list"; // Adjust according to your server API
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
