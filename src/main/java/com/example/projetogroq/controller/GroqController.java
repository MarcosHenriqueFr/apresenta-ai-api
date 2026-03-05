package com.example.projetogroq.controller;

import com.example.projetogroq.dto.groq.GroqResponseDTO;
import com.example.projetogroq.dto.input.PresentationRequestDTO;
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

    @PostMapping("/text")
    public ResponseEntity<GroqResponseDTO> generateSlides(@RequestBody PresentationRequestDTO dto){
        GroqResponseDTO response = groqService.generatePresentation(dto);

        // TODO: Fazer um PresentationResponseDTO
        System.out.println(response.choices().getFirst().message().content());

        return ResponseEntity.ok(response);
    }
}
