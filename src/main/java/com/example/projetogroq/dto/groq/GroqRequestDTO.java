package com.example.projetogroq.dto.groq;

import java.util.List;
import java.util.Map;

public record GroqRequestDTO(
        String model,
        List<MessageDTO> messages,
        ResponseFormatDTO response_format
) { }
