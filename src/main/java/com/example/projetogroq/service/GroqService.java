package com.example.projetogroq.service;

import com.example.projetogroq.dto.OutputQuality;
import com.example.projetogroq.dto.SlideLevel;
import com.example.projetogroq.dto.groq.GroqResponseDTO;
import com.example.projetogroq.dto.groq.ResponseFormatDTO;
import com.example.projetogroq.dto.input.PresentationRequestDTO;
import com.example.projetogroq.dto.groq.MessageDTO;
import com.example.projetogroq.dto.groq.GroqRequestDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class GroqService {

    private String model;
    private final WebClient webClient;

    public GroqService(WebClient webClient){
        this.webClient = webClient;
    }

    // O bodyValue() já faz a conversão de dto para objeto JSON
    // Já que está sendo feito um post request na API, por isso que também se usa .bodyValue()
    public GroqResponseDTO generatePresentation(PresentationRequestDTO dto){

        String context = createContext(dto);

        setModelByLevel(dto);

        GroqRequestDTO request = new GroqRequestDTO(
                model,
                List.of(new MessageDTO("user", context)),
                buildResponseFormat()
        );

        return webClient
                .post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GroqResponseDTO.class)
                .block();
    }

    // TODO: Criar os builds de Response format e de Messages do Groq
    private ResponseFormatDTO buildResponseFormat() {
        return null;
    }

    private void setModelByLevel(PresentationRequestDTO dto) {
        if(dto.quality().equals(OutputQuality.PREMIUM)){
            model = "openai/gpt-oss-120b";
        } else {
            model = "openai/gpt-oss-20b";
        }
    }

    private String createContext(PresentationRequestDTO dto){
        return """
                Você é um criador experiente em slides independentemente do assunto,
                preciso que crie slides com bullets e pequenos textos, sempre com exatidão de dados.
                Quero que faça um slide sobre o assunto %s, com uma duração de apresentação de %d minutos,
                que o nível de detalhamento seja %s, mantendo um slide para referências bibliográficas.
                É extremamente essencial que sua resposta siga esse modelo JSON, com um title geral e uma lista de slides:
                {
                  "title": "...",
                  "slides": [
                    {
                      "title": "...",
                      "bullets": ["...", "..."]
                    }
                  ]
                }
                """
                .formatted(dto.topic(), dto.durationInMinutes(), dto.level().toString());
    }
}
