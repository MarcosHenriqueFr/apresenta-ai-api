package com.example.projetogroq.dto.groq;

public record ResponseFormatDTO(
        String type,
        JsonSchemaDTO json_schema
) {
}
