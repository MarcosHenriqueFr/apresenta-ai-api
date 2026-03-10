package com.example.projetogroq.controller;

import com.example.projetogroq.dto.input.DownloadRequestDTO;
import com.example.projetogroq.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService){
        this.fileService = fileService;
    }

    @PostMapping("/pptx")
    public ResponseEntity<byte[]> downloadPptx(HttpServletRequest request, @RequestBody DownloadRequestDTO dto) throws IOException {
        HttpSession session = request.getSession(false);
        byte[] pptContent = fileService.downloadPptxFile(session, dto);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(pptContent.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pptContent);
    }
}
