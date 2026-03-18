package com.example.projetogroq.controller;

import com.example.projetogroq.dto.input.PresentationRequestDTO;
import com.example.projetogroq.dto.output.PresentationResponseDTO;
import com.example.projetogroq.service.GroqService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/presentation")
@Validated
public class GroqController {

    private static final Logger logger = LoggerFactory.getLogger(GroqController.class);

    private final GroqService groqService;

    public GroqController(GroqService groqService) {
        this.groqService = groqService;
    }

    // TODO: Adicionar validação de tamanho e de existência de pdf com validator
    @PostMapping(value = "/generate", consumes = "multipart/form-data")
    public ResponseEntity<PresentationResponseDTO> generateSlides(
            HttpServletRequest request,
            @Valid @RequestPart("data") PresentationRequestDTO dto,
            @RequestPart(value = "pdfs", required = false) List<MultipartFile> pdfs
            ) {
        HttpSession session = request.getSession(true);

        PresentationResponseDTO response =
                groqService.generatePresentation(session, dto, pdfs);

        logger.info("Created session: {}", session.getId());
        logger.info("Presentation was saved successfully.");

        return ResponseEntity.ok(response);
    }
}
