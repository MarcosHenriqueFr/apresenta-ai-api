package com.example.projetogroq.controller;

import com.example.projetogroq.dto.input.DownloadRequestDTO;
import com.example.projetogroq.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/files")
@Validated
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;

    public FileController(FileService fileService){
        this.fileService = fileService;
    }

    /**
     * Essencial para o download de arquivo .pptx com a estilização escolhida.
     * @param request Objeto para acessar a sessão
     * @param dto Contém informações sobre a estilização do slide
     * @return O arquivo de .pptx para download formatado corretamente pelos Headers
     * @throws IOException Caso o template do slide não exista nos recursos do servidor
     */
    @PostMapping("/pptx")
    public ResponseEntity<byte[]> downloadPptx(HttpServletRequest request, @Valid @RequestBody DownloadRequestDTO dto) throws IOException {

        // Define o arquivo final
        HttpSession session = request.getSession(false);
        byte[] pptContent = fileService.downloadPptxFile(session, dto);

        logger.info("File .pptx created successfully by: {}", session.getId());

        // Define a nomenclatura do arquivo final
        String filename = generateFilename(dto);
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);

        // Define as regras para esse arquivo final
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

    // Define o nome do arquivo de acordo com o estilo selecionado
    private String generateFilename(DownloadRequestDTO dto) {
        String style = dto.style() != null ? dto.style().toLowerCase() : "default";

        return String.format("presentation_%s.pptx", style);
    }
}
