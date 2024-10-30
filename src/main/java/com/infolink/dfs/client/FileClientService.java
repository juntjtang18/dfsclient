package com.infolink.dfs.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.infolink.dfs.client.FileClientController.RequestUpload;
import com.infolink.dfs.client.FileClientController.UploadResponse;
import com.infolink.dfs.shared.DfsFile;

import jakarta.annotation.PostConstruct;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FileClientService {
    private static final Logger logger = LoggerFactory.getLogger(FileClientService.class);

    @Value("${download.dir}")
    private String downloadRoot; // Will be injected from application.properties
    @Autowired
    private RestTemplate restTemplate;
    private String metaNodeUrl;
    private String USER = "user";
    
    @PostConstruct
    public void postConstruct() {
    	this.metaNodeUrl = "http://localhost:8080";
    }
    
    public String uploadFileToServer(MultipartFile file) {
        String targetDir = "/upload";
        String uploadUrl = getUploadUrl(file.getOriginalFilename(), targetDir);
        logger.info("Upload URL: {}", uploadUrl); // Debugging log
        
        if (uploadUrl.equals("File already exists at the given location.")) {
        	return "File already exists at the given location.";
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Prepare request entity with file and parameters
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        try {
            // Wrap the file as a ByteArrayResource
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            body.add("file", fileResource);
            body.add("user", USER);
            body.add("targetDir", targetDir);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Call the upload endpoint
            ResponseEntity<String> response = restTemplate.exchange(
                uploadUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
            );

            return response.getBody();
        } catch (IOException e) {
            logger.error("Error reading file bytes: {}", e.getMessage());
            throw new RuntimeException("File upload failed due to an error reading the file.");
        }
    }

    
    String getUploadUrl(String filename, String targetDir) {
        // Prepare the request body using RequestUpload class
        RequestUpload requestBody = new RequestUpload("uuid-1234", filename, targetDir, "owner1");

        // Set headers to indicate JSON content
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RequestUpload> request = new HttpEntity<>(requestBody, headers);

        // Send POST request and handle response
        ResponseEntity<UploadResponse> response = restTemplate.exchange(
            metaNodeUrl + "/metadata/upload-url", HttpMethod.POST, request, UploadResponse.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            UploadResponse uploadResponse = response.getBody();
            if (uploadResponse != null && !uploadResponse.isExists()) {
                return uploadResponse.getNodeUrl(); // Return the real node URL
            } else {
                return "File already exists at the given location.";
            }
        } else {
            return "Failed to retrieve upload URL: " + response.getStatusCode();
        }
    }    
    
    public String uploadFileToServer_old(MultipartFile file) {
        // Step 1: Get the upload URL from the metadata server
        String uploadUrl = getUploadUrl(file.getOriginalFilename(), "");
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


    public List<DfsFile> getFileListFromServer(String directory) {
        String url = metaNodeUrl + "/metadata/file/list"; // Adjusted to match the metanode endpoint

        // Creating a RequestDirectory object with the specified directory
        RequestDirectory requestDirectory = new RequestDirectory(directory);
        HttpEntity<RequestDirectory> requestEntity = new HttpEntity<>(requestDirectory);

        try {
            ResponseEntity<DfsFile[]> response = restTemplate.exchange(
                url, HttpMethod.POST, requestEntity, DfsFile[].class
            );

            // Check for success and return the body
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return Arrays.asList(response.getBody());
            } else {
                logger.error("Failed to fetch file list from server. Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to fetch file list from server.");
            }
        } catch (RestClientException e) {
            logger.error("Exception while fetching file list from server: {}", e.getMessage());
            throw new RuntimeException("Error occurred while retrieving file list.", e);
        }
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

    // Inner class to represent the request for upload
    public static class RequestUpload {
        private String uuid; // UUID of the request
        private String filename; // Filename to upload
        private String targetDir;
        private String owner;
        
        public RequestUpload() {}

        public RequestUpload(String uuid, String filename, String targetDir, String owner) {
            this.uuid = uuid;
            this.filename = filename;
            this.setTargetDir(targetDir);
            this.setOwner(owner);
        }

        public String getUuid() 					{            return uuid;        }
        public String getFilename() 				{            return filename;        }
		public String getTargetDir() 				{			return targetDir;		}
		public void setTargetDir(String targetDir) 	{			this.targetDir = targetDir;		}
		public String getOwner() 					{			return owner;		}
		public void setOwner(String owner) 			{			this.owner = owner;		}
    }

    // Inner class to represent the response for file check
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UploadResponse {
        private boolean exists; // Flag indicating whether the file exists
        private String nodeUrl; // URL of the selected node
        
        public UploadResponse() {
        	this.exists = false;
        	this.nodeUrl = "";
        }

        // Constructor with @JsonCreator for Jackson
        @JsonCreator
        public UploadResponse(@JsonProperty("exists") boolean exists, @JsonProperty("nodeUrl") String nodeUrl) {
            this.exists = exists;
            this.nodeUrl = nodeUrl;
        }

        public boolean isExists() {            return exists;        }
        public String getNodeUrl() {            return nodeUrl;        }
    }
    /**
     * Inner class to represent a request containing the directory.
     */
    public static class RequestDirectory {
        private String directory;
        private String owner;
        public RequestDirectory() {}
        public RequestDirectory(String dir) {
        	this.directory = dir;
        	this.owner = System.getProperty("user.name");
        }
        // Getters and Setters
        public String getDirectory() 				{            return directory;        }
        public void setDirectory(String directory) 	{            this.directory = directory;        }
		public String getOwner() 					{			return owner;		}
		public void setOwner(String owner) 			{			this.owner = owner;		}
    }

}
