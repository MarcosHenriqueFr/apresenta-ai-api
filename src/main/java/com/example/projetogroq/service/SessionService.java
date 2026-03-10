package com.example.projetogroq.service;

import com.example.projetogroq.dto.output.PresentationResponseDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    // TODO: Criar exceptions mais específicas

    private static final String SESSION_ATT_PRESENTATION = "presentationData";

    void savePresentationData(HttpSession session, PresentationResponseDTO response) {
        session.setAttribute(SESSION_ATT_PRESENTATION, response);
    }

    PresentationResponseDTO getPresentationData(HttpSession session) {
        return (PresentationResponseDTO) session.getAttribute(SESSION_ATT_PRESENTATION);
    }

    void checkSessionExistence(HttpSession session) {
        if(session == null){
            throw new IllegalStateException("No active session found. Generate a presentation before.");
        }
    }
}
