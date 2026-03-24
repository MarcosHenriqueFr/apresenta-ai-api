package com.example.projetogroq.service;

import com.example.projetogroq.dto.enums.OutputQuality;
import com.example.projetogroq.dto.groq.*;
import com.example.projetogroq.dto.input.PresentationRequestDTO;
import com.example.projetogroq.dto.output.PresentationResponseDTO;
import com.example.projetogroq.exception.custom.GroqIllegalResponseException;
import com.example.projetogroq.exception.custom.GroqResponseParseException;
import com.example.projetogroq.exception.custom.GroqTooManyAttempsException;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
public class GroqService {

    private static final double INITIAL_TEMPERATURE = 0.5;
    private static final double TEMPERATURE_STEP = 0.1;
    private static final double MIN_TEMPERATURE = 0.2;
    private static final int MAX_RETRIES = 3;

    private static final Logger logger = LoggerFactory.getLogger(GroqService.class);

    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final SessionService sessionService;
    private final FileService fileService;

    public GroqService(
            WebClient webClient,
            ObjectMapper mapper,
            SessionService sessionService,
            FileService fileService
    ) {
        this.webClient = webClient;
        this.mapper = mapper;
        this.sessionService = sessionService;
        this.fileService = fileService;
    }

    // O bodyValue() já faz a conversão de dto para objeto JSON
    // Já que está sendo feito um post request na API, por isso que também se usa .bodyValue()

    /**
     * Para a construção da Apresentação a partir da resposta da API do Groq.
     * Orquestra a preparação do contexto, a chamada à API com retry e a persistência em sessão.
     *
     * @param dto Com as informações preenchidas pelo client.
     * @return {@link PresentationResponseDTO} convertido das informações do {@link GroqResponseDTO}
     */
    public PresentationResponseDTO generatePresentation(
            HttpSession session,
            PresentationRequestDTO dto,
            List<MultipartFile> pdfs
    ) {
        String context = buildPresentationContext(dto, pdfs);
        String model = chooseModel(dto.quality());

        PresentationResponseDTO response = callWithRetry(model, context);

        sessionService.savePresentationData(session, response);

        return response;
    }

    /**
     * Monta o contexto completo do prompt, combinando os dados do formulário com o conteúdo dos PDFs.
     *
     * @param dto  Com as informações preenchidas pelo client.
     * @param pdfs Arquivos opcionais enviados pelo client.
     * @return Contexto formatado para envio ao modelo.
     */
    private String buildPresentationContext(PresentationRequestDTO dto, List<MultipartFile> pdfs) {
        boolean available = fileService.checkPdfAvailability(pdfs);
        String pdfContext = fileService.getContextFromFiles(pdfs, available);
        return createContext(dto, pdfContext);
    }

    /**
     * Executa a requisição para a API externa com até {@link #MAX_RETRIES} tentativas.
     * Reduz a temperatura a cada falha com status 400 para diminuir alucinações.
     *
     * @param model   Modelo de IA escolhido com base na qualidade.
     * @param context Contexto completo do prompt.
     * @return {@link PresentationResponseDTO} válido.
     */
    private PresentationResponseDTO callWithRetry(String model, String context) {
        double temperature = INITIAL_TEMPERATURE;

        // Nunca faz attemp = 3, já que o throw para a execução do loop antes.
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                return executeApiCall(model, context, temperature);
            } catch (WebClientResponseException e) {
                boolean isStatusCode400 = e.getStatusCode().value() == 400;
                boolean isRetryPossible = attempt < MAX_RETRIES - 1;

                if (!isStatusCode400 || !isRetryPossible) {
                    throw e;
                }

                temperature = Math.max(temperature - TEMPERATURE_STEP, MIN_TEMPERATURE);
                logger.info("Retry Number {} for external api request.", attempt);
            }
        }

        throw new GroqTooManyAttempsException("Failed after too many retries.");
    }

    /**
     * Executa uma única chamada à API do Groq e converte a resposta.
     *
     * @param model       Modelo de IA.
     * @param context     Contexto do prompt.
     * @param temperature Parâmetro de criatividade da resposta.
     * @return {@link PresentationResponseDTO} parseado da resposta.
     */
    private PresentationResponseDTO executeApiCall(String model, String context, double temperature) {
        GroqRequestDTO request = createRequest(model, context, temperature);

        GroqResponseDTO groqResponse = webClient
                .post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GroqResponseDTO.class)
                .blockOptional()
                .orElseThrow(() -> new IllegalStateException("Groq returned empty response"));

        return convertApiResponse(groqResponse);
    }

    /**
     * Converte a resposta da API externa em uma da API interna para o armazenamento em Session
     *
     * @param groqResponse - Vem diretamente da API externa
     * @return Um modelo válido da API interna para um client consumidor.
     */
    private PresentationResponseDTO convertApiResponse(GroqResponseDTO groqResponse) {

        boolean isChoiceNull = groqResponse.choices() == null;

        if (isChoiceNull || groqResponse.choices().isEmpty()) {
            throw new GroqIllegalResponseException("Groq returned no choices");
        }

        try {
            String contentJson = groqResponse.choices().getFirst().message().content();
            return mapper.readValue(contentJson, PresentationResponseDTO.class);
        } catch (Exception e) {
            throw new GroqResponseParseException("Failed to parse groq response.");
        }
    }

    /**
     * Cria a request seguindo o modelo JSON da API do Groq.
     *
     * @param model       definido pela qualidade acessível ao usuário
     * @param context     definido pelas informações preenchidas pelo client.
     * @param temperature um parâmetro que define a quantidade de alucinação/criatividade aceitável na resposta
     * @return Uma {@link GroqRequestDTO} viável para envio à API
     */
    private GroqRequestDTO createRequest(String model, String context, double temperature) {
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
     *
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
     *
     * @param quality Com as informações preenchidas pelo client.
     * @return O modelo de IA
     */
    private String chooseModel(String quality) {
        OutputQuality desiredQuality = OutputQuality.valueOf(quality);

        return desiredQuality == OutputQuality.PREMIUM
                ? "openai/gpt-oss-120b"
                : "openai/gpt-oss-20b";
    }

    /**
     * Define todas as limitações que a IA deve seguir para evitar alucinações de dados estatísticos.
     *
     * @param dto Com as inforamações preenchidas pelo client.
     * @return Um contexto válido para ser colocado dentro do {@link GroqRequestDTO}
     */
    private String createContext(PresentationRequestDTO dto, String pdfContext) {
        return """
                Crie slides em formato profissional com bullet points claros e organizados.
                
                IMPORTANTE:
                - Não invente estatísticas específicas ou valores numéricos exatos.
                - Caso não tenha dados confirmáveis, use descrições qualitativas.
                - Não crie referências fictícias, principalmente estudos de casos imaginários.
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
                
                %s
                """
                .formatted(dto.topic(), dto.durationInMinutes(), dto.level(), pdfContext);
    }
}
