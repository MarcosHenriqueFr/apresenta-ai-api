package com.example.projetogroq.dto.input;

import com.example.projetogroq.dto.SlideStyle;

// TODO: Validação para enums com validator
public record DownloadRequestDTO(
        SlideStyle style
) { }
