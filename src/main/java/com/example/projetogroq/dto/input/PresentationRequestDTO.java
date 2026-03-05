package com.example.projetogroq.dto.input;

import com.example.projetogroq.dto.OutputQuality;
import com.example.projetogroq.dto.SlideLevel;

public record PresentationRequestDTO(
    String topic,
    int durationInMinutes,
    SlideLevel level,
    OutputQuality quality,
    String style
) { }
