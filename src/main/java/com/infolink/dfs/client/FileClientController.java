package com.infolink.dfs.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.infolink.dfs.shared.DfsFile;


@Controller
public class FileClientController {
    private static final Logger logger = LoggerFactory.getLogger(FileClientService.class);

    private final FileClientService fileClientService;

    public FileClientController(FileClientService fileClientService) {
        this.fileClientService = fileClientService;
    }

    @GetMapping("/")
    public String homePage() {
        return "index"; // returns index.html
    }

    @GetMapping("/upload")
    public String uploadPage() {
        return "upload"; // returns upload.html
    }

    // Upload a file via the client
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
    	String username = System.getProperty("user.name");
    	
        String response = fileClientService.uploadFileToServer(username, file, "/");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download")
    public ResponseEntity<String> downloadFile(@RequestParam("filename") String filename) {
        logger.info("Get/download called for filename: {}", filename);
        
        try {
            fileClientService.downloadFileFromServer(filename); // Download the file from the server

            // Return a success message or the path of the downloaded file
            return ResponseEntity.ok("File downloaded successfully: " + filename); // or the full path if you prefer
        } catch (Exception e) {
            logger.error("Error downloading file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to download file: " + e.getMessage());
        }
    }

    @GetMapping("/file-list")
    public String getFileList(Model model) {
        List<DfsFile> files = fileClientService.getFileListFromServer("/upload");
        model.addAttribute("files", files);
        return "file-list";
    }
    
    @GetMapping("/upload-directory")
    public String uploadDirectoryPage() {
        return "upload-directory"; // returns upload-directory.html
    }

    @PostMapping("/upload-directory")
    public ResponseEntity<String> uploadDirectory(@RequestParam("directory") String directoryPath, @RequestParam("targetDfsDir") String targetDfsDir) {
        try {
            String username = System.getProperty("user.name");
            fileClientService.uploadFilesFromDirectory(username, directoryPath, targetDfsDir);
            return ResponseEntity.ok("All files uploaded successfully from: " + directoryPath + " to " + targetDfsDir);
        } catch (Exception e) {
            logger.error("Error uploading directory: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Failed to upload directory: " + e.getMessage());
        }
    }

    
    // Inner class to represent the request for upload
    public static class RequestUpload {
        private String uuid; // UUID of the request
        private String filename; // Filename to upload

        public RequestUpload() {
        }

        public RequestUpload(String uuid, String filename) {
            this.uuid = uuid;
            this.filename = filename;
        }

        public String getUuid() {
            return uuid;
        }

        public String getFilename() {
            return filename;
        }
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

        public boolean isExists() {
            return exists;
        }

        public String getNodeUrl() {
            return nodeUrl;
        }
    }
}
