package com.example.projetogroq.service;

import com.example.projetogroq.dto.output.PresentationResponseDTO;
import com.example.projetogroq.exception.custom.IllegalSessionStateException;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceTest {

    @Mock
    private HttpSession session;

    private AutoCloseable closeable;
    private SessionService sessionService;

    @BeforeEach
    void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        sessionService = new SessionService();
    }

    @AfterEach
    void closeMocks() throws Exception {
        closeable.close();
    }

    @Test
    @DisplayName("Should save presentation data using the correct session attribute key")
    void savePresentationDataSuccess() {
        PresentationResponseDTO response = new PresentationResponseDTO("Title", List.of());

        sessionService.savePresentationData(session, response);

        verify(session).setAttribute("presentationData", response);
    }

    @Test
    @DisplayName("Should return presentation data stored in session")
    void getPresentationDataSuccess() {
        PresentationResponseDTO expected = new PresentationResponseDTO("Title", List.of());
        when(session.getAttribute("presentationData")).thenReturn(expected);

        PresentationResponseDTO result = sessionService.getPresentationData(session);

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should throw IllegalSessionStateException when session is null")
    void checkSessionExistenceThrowsWhenSessionIsNull() {
        assertThrows(IllegalSessionStateException.class, () ->
                sessionService.checkSessionExistence(null)
        );
    }

    @Test
    @DisplayName("Should not throw when session is active")
    void checkSessionExistenceDoesNotThrowWhenSessionIsValid() {
        assertDoesNotThrow(() -> sessionService.checkSessionExistence(session));
    }
}