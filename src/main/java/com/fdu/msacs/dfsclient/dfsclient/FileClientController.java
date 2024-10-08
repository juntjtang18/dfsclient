package com.fdu.msacs.dfsclient.dfsclient;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class FileClientController {

    private final FileClientService fileClientService;

    @GetMapping("/")
    public String homePage() {
        return "index"; // returns index.html
    }

    @GetMapping("/upload")
    public String uploadPage() {
        return "upload"; // returns upload.html
    }

    
    public FileClientController(FileClientService fileClientService) {
        this.fileClientService = fileClientService;
    }

    // Upload a file via the client
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String response = fileClientService.uploadFileToServer(file);
        return ResponseEntity.ok(response);
    }

    

    @GetMapping("/download")
    public String downloadPage() {
        return "download"; // returns download.html
    }
    
    @GetMapping("/download/{filename}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String filename) {
        byte[] fileData = fileClientService.downloadFileFromServer(filename);
        if (fileData == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(fileData);
    }
}
