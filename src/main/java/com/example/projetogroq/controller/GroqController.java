package com.example.projetogroq.controller;

import com.example.projetogroq.dto.input.PresentationRequestDTO;
import com.example.projetogroq.dto.output.PresentationResponseDTO;
import com.example.projetogroq.service.GroqService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GroqController {

    private final GroqService groqService;

    public GroqController(GroqService groqService){
        this.groqService = groqService;
    }

    @PostMapping("/generate")
    public ResponseEntity<PresentationResponseDTO> generateSlides(@RequestBody PresentationRequestDTO dto){
        PresentationResponseDTO response = groqService.generatePresentation(dto);
        return ResponseEntity.ok(response);
    }
}
