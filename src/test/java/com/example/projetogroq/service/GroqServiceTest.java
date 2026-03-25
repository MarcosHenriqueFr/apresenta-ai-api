package com.example.projetogroq.service;

import com.example.projetogroq.dto.groq.ChoiceDTO;
import com.example.projetogroq.dto.groq.GroqResponseDTO;
import com.example.projetogroq.dto.groq.MessageDTO;
import com.example.projetogroq.dto.input.PresentationRequestDTO;
import com.example.projetogroq.dto.output.PresentationResponseDTO;
import com.example.projetogroq.exception.custom.GroqIllegalResponseException;
import com.example.projetogroq.exception.custom.GroqResponseParseException;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroqServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient webClient;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private SessionService sessionService;

    @Mock
    private FileService fileService;

    @Mock
    private HttpSession session;

    private AutoCloseable closeable;
    private GroqService groqService;

    @BeforeEach
    void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        groqService = new GroqService(webClient, mapper, sessionService, fileService);
    }

    @AfterEach
    void closeMocks() throws Exception {
        closeable.close();
    }

    @Test
    @DisplayName("Should generate presentation, return response and save it to session")
    void generatePresentationSuccess() {
        PresentationRequestDTO dto = new PresentationRequestDTO("AI", 10, "BASIC", "STANDARD");
        String contentJson = "{\"title\":\"AI\",\"slides\":[]}";
        PresentationResponseDTO expected = new PresentationResponseDTO("AI", List.of());
        GroqResponseDTO groqResponse = new GroqResponseDTO(
                List.of(new ChoiceDTO(new MessageDTO("assistant", contentJson)))
        );

        when(fileService.checkPdfAvailability(any())).thenReturn(false);
        when(fileService.getContextFromFiles(any(), eq(false))).thenReturn("");
        when(webClient.post()
                .uri(anyString())
                .bodyValue(any())
                .retrieve()
                .bodyToMono(GroqResponseDTO.class))
                .thenReturn(Mono.just(groqResponse));
        when(mapper.readValue(contentJson, PresentationResponseDTO.class)).thenReturn(expected);

        PresentationResponseDTO result = groqService.generatePresentation(session, dto, null);

        assertEquals(expected, result);
        verify(sessionService).savePresentationData(session, expected);
    }

    @Test
    @DisplayName("Should retry on HTTP 400 and succeed on the second attempt")
    void generatePresentationRetriesOn400AndSucceeds() {
        PresentationRequestDTO dto = new PresentationRequestDTO("AI", 10, "BASIC", "STANDARD");
        String contentJson = "{\"title\":\"AI\",\"slides\":[]}";
        PresentationResponseDTO expected = new PresentationResponseDTO("AI", List.of());
        GroqResponseDTO groqResponse = new GroqResponseDTO(
                List.of(new ChoiceDTO(new MessageDTO("assistant", contentJson)))
        );
        WebClientResponseException exception400 = WebClientResponseException.create(
                400, "Bad Request", null, null, null
        );

        when(fileService.checkPdfAvailability(any())).thenReturn(false);
        when(fileService.getContextFromFiles(any(), eq(false))).thenReturn("");
        when(webClient.post()
                .uri(anyString())
                .bodyValue(any())
                .retrieve()
                .bodyToMono(GroqResponseDTO.class))
                .thenThrow(exception400)
                .thenReturn(Mono.just(groqResponse));
        when(mapper.readValue(contentJson, PresentationResponseDTO.class)).thenReturn(expected);

        PresentationResponseDTO result = groqService.generatePresentation(session, dto, null);

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should rethrow WebClientResponseException immediately on non-400 status")
    void generatePresentationRethrowsOnNon400Status() {
        PresentationRequestDTO dto = new PresentationRequestDTO("AI", 10, "BASIC", "STANDARD");
        WebClientResponseException exception500 = WebClientResponseException.create(
                500, "Internal Server Error", null, null, null
        );

        when(fileService.checkPdfAvailability(any())).thenReturn(false);
        when(fileService.getContextFromFiles(any(), eq(false))).thenReturn("");
        when(webClient.post()
                .uri(anyString())
                .bodyValue(any())
                .retrieve()
                .bodyToMono(GroqResponseDTO.class))
                .thenThrow(exception500);

        assertThrows(WebClientResponseException.class, () ->
                groqService.generatePresentation(session, dto, null)
        );
    }

    @Test
    @DisplayName("Should throw GroqIllegalResponseException when choices are null in response")
    void generatePresentationThrowsWhenChoicesAreNull() {
        PresentationRequestDTO dto = new PresentationRequestDTO("AI", 10, "BASIC", "STANDARD");
        GroqResponseDTO groqResponse = new GroqResponseDTO(null);

        when(fileService.checkPdfAvailability(any())).thenReturn(false);
        when(fileService.getContextFromFiles(any(), eq(false))).thenReturn("");
        when(webClient.post()
                .uri(anyString())
                .bodyValue(any())
                .retrieve()
                .bodyToMono(GroqResponseDTO.class))
                .thenReturn(Mono.just(groqResponse));

        assertThrows(GroqIllegalResponseException.class, () ->
                groqService.generatePresentation(session, dto, null)
        );
    }

    @Test
    @DisplayName("Should throw GroqIllegalResponseException when choices list is empty")
    void generatePresentationThrowsWhenChoicesAreEmpty() {
        PresentationRequestDTO dto = new PresentationRequestDTO("AI", 10, "BASIC", "STANDARD");
        GroqResponseDTO groqResponse = new GroqResponseDTO(List.of());

        when(fileService.checkPdfAvailability(any())).thenReturn(false);
        when(fileService.getContextFromFiles(any(), eq(false))).thenReturn("");
        when(webClient.post()
                .uri(anyString())
                .bodyValue(any())
                .retrieve()
                .bodyToMono(GroqResponseDTO.class))
                .thenReturn(Mono.just(groqResponse));

        assertThrows(GroqIllegalResponseException.class, () ->
                groqService.generatePresentation(session, dto, null)
        );
    }

    @Test
    @DisplayName("Should throw GroqResponseParseException when response content cannot be parsed")
    void generatePresentationThrowsOnParseFailure() {
        PresentationRequestDTO dto = new PresentationRequestDTO("AI", 10, "BASIC", "STANDARD");
        String malformedJson = "not_valid_json";
        GroqResponseDTO groqResponse = new GroqResponseDTO(
                List.of(new ChoiceDTO(new MessageDTO("assistant", malformedJson)))
        );

        when(fileService.checkPdfAvailability(any())).thenReturn(false);
        when(fileService.getContextFromFiles(any(), eq(false))).thenReturn("");
        when(webClient.post()
                .uri(anyString())
                .bodyValue(any())
                .retrieve()
                .bodyToMono(GroqResponseDTO.class))
                .thenReturn(Mono.just(groqResponse));
        when(mapper.readValue(malformedJson, PresentationResponseDTO.class))
                .thenThrow(new RuntimeException("Parse error"));

        assertThrows(GroqResponseParseException.class, () ->
                groqService.generatePresentation(session, dto, null)
        );
    }
}