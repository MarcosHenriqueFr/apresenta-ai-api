package com.example.projetogroq.dto.output;

import java.util.List;

public record PresentationResponseDTO(
    String title,
    List<SlideDTO> slides
) { }