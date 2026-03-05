package com.example.projetogroq.dto.groq;

import java.util.Map;

public record JsonSchemaDTO(
        String name,
        boolean strict,
        Map<String, Object> schema
) {
}
