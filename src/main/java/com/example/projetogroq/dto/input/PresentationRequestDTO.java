package com.example.projetogroq.dto.input;

import com.example.projetogroq.dto.validator.ValidOutputQuality;
import com.example.projetogroq.dto.validator.ValidSlideLevel;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;

public record PresentationRequestDTO(

        @NotEmpty(message = "Topic should have content.")
        String topic,

        @NotNull(message = "Should inform presentation duration.")
        @Range(min = 5, message = "Presentation should be at least 5 minutes long.")
        Integer durationInMinutes,

        @NotNull(message = "Level should pass in request.")
        @ValidSlideLevel
        String level,

        @NotNull(message = "Quality should not be null.")
        @ValidOutputQuality
        String quality
) {
}
