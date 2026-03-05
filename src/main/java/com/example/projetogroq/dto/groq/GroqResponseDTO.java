package com.example.projetogroq.dto.groq;

import java.util.List;

public record GroqResponseDTO(
    List<ChoiceDTO> choices
) { }
