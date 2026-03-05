package com.example.projetogroq.dto.output;

import java.util.List;

public record SlideDTO(
        String title,
        List<String> bullets
) { }
