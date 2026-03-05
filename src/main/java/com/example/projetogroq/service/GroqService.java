package com.example.projetogroq.service;

import com.example.projetogroq.dto.OutputQuality;
import com.example.projetogroq.dto.groq.*;
import com.example.projetogroq.dto.input.PresentationRequestDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class GroqService {

    private final WebClient webClient;

    public GroqService(WebClient webClient){
        this.webClient = webClient;
    }

    // O bodyValue() já faz a conversão de dto para objeto JSON
    // Já que está sendo feito um post request na API, por isso que também se usa .bodyValue()
    public GroqResponseDTO generatePresentation(PresentationRequestDTO dto){

        double temperature = 0.7;
        int maxRetries = 3;

        String context = createContext(dto);

        String model = chooseModel(dto);

        for(int attempt = 0; attempt < maxRetries; attempt++){
            try {
                GroqRequestDTO request = createRequest(model, context, temperature);

                return webClient
                        .post()
                        .uri("/chat/completions")
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(GroqResponseDTO.class)
                        .block();
            } catch(WebClientResponseException e){
                boolean isStatusCode400 = e.getStatusCode().value() == 400;
                boolean isAttemptPossible = attempt < maxRetries - 1;

                if(isStatusCode400 && isAttemptPossible){
                    temperature = Math.max(temperature - 0.2, 0.2);
                    continue;
                }

                throw e;
            }
        }

        throw new RuntimeException("Failed after too many retries.");
    }

    private GroqRequestDTO createRequest(String model, String context, double temperature){
        return new GroqRequestDTO(
                model,
                buildMessages(context),
                buildResponseFormat(),
                temperature
        );
    }

    private List<MessageDTO> buildMessages(String context) {
        return List.of(
                new MessageDTO("system", "Você é excelente na criação de slides de forma profissional."),
                new MessageDTO("user", context)
        );
    }

    private ResponseFormatDTO buildResponseFormat() {
        return new ResponseFormatDTO(
                "json_schema",
                buildJsonSchema()
        );
    }

    private JsonSchemaDTO buildJsonSchema() {
        return new JsonSchemaDTO(
                "slide_presentation",
                true,
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "title", Map.of("type", "string"),
                        "slides", Map.of(
                            "type", "array",
                            "items", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "title", Map.of("type", "string"),
                                        "bullets", Map.of(
                                                "type", "array",
                                                "items", Map.of("type", "string")
                                        )
                                    ),
                                "required", List.of("title", "bullets"),
                                "additionalProperties", false
                                )
                            )
                        ),
                    "required", List.of("title", "slides"),
                    "additionalProperties", false
                )
        );
    }

    private String chooseModel(PresentationRequestDTO dto) {
        return dto.quality() == OutputQuality.PREMIUM
                ? "openai/gpt-oss-120b"
                : "openai/gpt-oss-20b";
    }

    private String createContext(PresentationRequestDTO dto){
        return """
                Crie slides em formato profissional com bullet points claros e organizados.
                
                IMPORTANTE:
                - Não invente estatísticas específicas ou valores numéricos exatos.
                - Caso não tenha dados confirmáveis, use descrições qualitativas.
                - Não crie referências fictícias.
                - Se mencionar fontes, cite apenas instituições conhecidas sem criar links específicos.
                
                Tema: %s
                Duração total: %d minutos
                Nível de detalhamento: %s
                
                Quanto maior o tempo, maior a quantidade de slides.
                Inclua um slide final com o título "Referências Bibliográficas".
                
                Responda apenas com JSON válido conforme o schema definido.
                Não inclua explicações adicionais.
                Não use markdown.
                Não use blocos de código.
                Não inclua texto antes ou depois.
                """
                .formatted(dto.topic(), dto.durationInMinutes(), dto.level().toString());
    }
}
