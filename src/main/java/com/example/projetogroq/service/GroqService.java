package com.example.projetogroq.service;

import com.example.projetogroq.dto.OutputQuality;
import com.example.projetogroq.dto.groq.*;
import com.example.projetogroq.dto.input.PresentationRequestDTO;
import com.example.projetogroq.dto.output.PresentationResponseDTO;
import com.example.projetogroq.exception.GroqIllegalResponseException;
import com.example.projetogroq.exception.GroqResponseParseException;
import com.example.projetogroq.exception.GroqTooManyAttempsException;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
public class GroqService {

    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final SessionService sessionService;

    public GroqService(WebClient webClient, ObjectMapper mapper, SessionService sessionService){
        this.webClient = webClient;
        this.mapper = mapper;
        this.sessionService = sessionService;
    }

    // O bodyValue() já faz a conversão de dto para objeto JSON
    // Já que está sendo feito um post request na API, por isso que também se usa .bodyValue()

    /**
     * Para a construção da Apresentação a partir da resposta da API do Groq.
     * Executa a requisição para a API externa 3 vezes e retorna um erro caso esse critério não seja atendido. <br>
     * Cria o content da mensagem do Groq com os dados do {@link PresentationRequestDTO}.
     * @param dto Com as informações preenchidas pelo client.
     * @return {@link PresentationResponseDTO} convertido das informações do {@link GroqResponseDTO}
     */
    public PresentationResponseDTO generatePresentation(HttpSession session, PresentationRequestDTO dto){

        double temperature = 0.7;
        int maxRetries = 3;

        String context = createContext(dto);

        String model = chooseModel(dto);

        for(int attempt = 0; attempt < maxRetries; attempt++){
            try {
                GroqRequestDTO request = createRequest(model, context, temperature);

                GroqResponseDTO groqResponse = webClient
                        .post()
                        .uri("/chat/completions")
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(GroqResponseDTO.class)
                        .blockOptional()
                        .orElseThrow(() -> new IllegalStateException("Groq returned empty response"));

                PresentationResponseDTO response = convertApiResponse(groqResponse);

                sessionService.savePresentationData(session, response);

                return response;

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

        throw new GroqTooManyAttempsException("Failed after too many retries.");
    }

    /**
     * Converte a resposta da API externa em uma da API interna para o armazenamento em Session
     * @param groqResponse - Vem diretamente da API externa
     * @return Um modelo válido da API interna para um client consumidor.
     */
    private PresentationResponseDTO convertApiResponse(GroqResponseDTO groqResponse) {

        boolean isChoiceNull = groqResponse.choices() == null;

        if (isChoiceNull || groqResponse.choices().isEmpty()){
            throw new GroqIllegalResponseException("Groq returned no choices");
        }

        try {
            String contentJson = groqResponse.choices().getFirst().message().content();
            return mapper.readValue(contentJson, PresentationResponseDTO.class);
        } catch (Exception e){
            throw new GroqResponseParseException("Failed to parse groq response.");
        }
    }

    /**
     * Cria a request seguindo o modelo JSON da API do Groq.
     * @param model definido pela qualidade acessível ao usuário
     * @param context definido pelas informações preenchidas pelo client.
     * @param temperature um parâmetro que define a quantidade de alucinação/criatividade aceitável na resposta
     * @return Uma {@link GroqRequestDTO} viável para envio à API
     */
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

    /**
     * Essencial para construir um Json válido para a API do Groq com o uso de um {@link ResponseFormatDTO}
     * @return Um response format com atributos stricts para retorno de JSON válido.
     */
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

    /**
     * Escolhe o modelo de IA do Groq com base na qualidade de output,
     * esse valor precisa ser movido posteriormente
     * quando tiver um sistema de autenticação e roles de usuário.
     * @param dto Com as informações preenchidas pelo client.
     * @return O modelo de IA
     */
    private String chooseModel(PresentationRequestDTO dto) {
        return dto.quality() == OutputQuality.PREMIUM
                ? "openai/gpt-oss-120b"
                : "openai/gpt-oss-20b";
    }

    /**
     * Define todas as limitações que a IA deve seguir para evitar alucinações de dados estatísticos.
     * @param dto Com as inforamações preenchidas pelo client.
     * @return Um contexto válido para ser colocado dentro do {@link GroqRequestDTO}
     */
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
                Inclua um slide inicial com o título "Introdução", contextualizando o tema.
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
