package com.example.projetogroq.controller;

import com.example.projetogroq.dto.input.PresentationRequestDTO;
import com.example.projetogroq.dto.output.PresentationResponseDTO;
import com.example.projetogroq.service.GroqService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/presentation")
public class GroqController {

    private static final Logger logger = LoggerFactory.getLogger(GroqController.class);

    private final GroqService groqService;

    public GroqController(GroqService groqService){
        this.groqService = groqService;
    }

    @PostMapping("/generate")
    public ResponseEntity<PresentationResponseDTO> generateSlides(HttpServletRequest request, @RequestBody PresentationRequestDTO dto){
        HttpSession session = request.getSession(true);

        PresentationResponseDTO response = groqService.generatePresentation(session, dto);

        logger.info("Sessão criada: {}", session.getId());
        logger.info("Apresentação salva na sessão com sucesso.");

        return ResponseEntity.ok(response);
    }
}
