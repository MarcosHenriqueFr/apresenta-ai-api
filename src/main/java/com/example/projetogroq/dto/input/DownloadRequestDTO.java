package com.example.projetogroq.dto.input;

import com.example.projetogroq.dto.validator.ValidSlideStyle;
import jakarta.validation.constraints.NotNull;

public record DownloadRequestDTO(

        @NotNull(message = "Should send style info.")
        @ValidSlideStyle
        String style
) { }
