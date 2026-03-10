package com.example.projetogroq.controller;

import com.example.projetogroq.dto.input.DownloadRequestDTO;
import com.example.projetogroq.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;

    public FileController(FileService fileService){
        this.fileService = fileService;
    }

    @PostMapping("/pptx")
    public ResponseEntity<byte[]> downloadPptx(HttpServletRequest request, @RequestBody DownloadRequestDTO dto) throws IOException {
        HttpSession session = request.getSession(false);
        byte[] pptContent = fileService.downloadPptxFile(session, dto);

        logger.info("File .pptx created successfully by: {}", session.getId());

        String filename = generateFilename(dto);
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(pptContent.length);
        headers.set(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(pptContent);
    }

    private String generateFilename(DownloadRequestDTO dto) {
        String style = dto.style() != null ? dto.style().name().toLowerCase() : "default";

        return String.format("presentation_%s.pptx", style);
    }
}
