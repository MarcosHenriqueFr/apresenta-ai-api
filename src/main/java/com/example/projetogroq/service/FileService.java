package com.example.projetogroq.service;

import com.example.projetogroq.dto.SlideStyle;
import com.example.projetogroq.dto.input.DownloadRequestDTO;
import com.example.projetogroq.dto.output.PresentationResponseDTO;
import com.example.projetogroq.utils.TemplateUtils;
import jakarta.servlet.http.HttpSession;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class FileService {

    // TODO: Criar exceptions mais específicas posteriormente

    private final SessionService sessionService;

    public FileService(SessionService sessionService){
        this.sessionService = sessionService;
    }

    public byte[] downloadPptxFile(HttpSession session, DownloadRequestDTO dto) throws IOException {
        PresentationResponseDTO presentation = getPresentationData(session);

        return createPptxFile(presentation, dto);
    }

    // Só retornando um title simples
    private byte[] createPptxFile(PresentationResponseDTO presentation, DownloadRequestDTO dto) throws IOException {
        try(XMLSlideShow ppt = checkSlideStyle(dto)){

            XSLFSlideMaster master = ppt.getSlideMasters().getFirst();
            XSLFSlideLayout layoutTitle = TemplateUtils.getLayoutTitle(master);

            XSLFSlide titleSlide = ppt.createSlide(layoutTitle);

            XSLFTextShape title = titleSlide.getPlaceholder(0);
            title.setText(presentation.title());

            try(ByteArrayOutputStream baos = new ByteArrayOutputStream()){
                ppt.write(baos);
                return baos.toByteArray();
            }
        }
    }

    private XMLSlideShow checkSlideStyle(DownloadRequestDTO dto) throws IOException {
        if (dto.style() == SlideStyle.ACADEMIC){
            return TemplateUtils.loadTemplateAcademic();
        } else if (dto.style() == SlideStyle.CREATIVE){
            return TemplateUtils.loadTemplateCreative();
        }

        return TemplateUtils.loadTemplateBasic();
    }

    private PresentationResponseDTO getPresentationData(HttpSession session){
        sessionService.checkSessionExistence(session);

        PresentationResponseDTO presentationDTO = sessionService.getPresentationData(session);
        checkPresentationExistence(presentationDTO);

        return presentationDTO;
    }

    private void checkPresentationExistence(PresentationResponseDTO presentation) {
        if(presentation == null){
            throw new IllegalStateException("No presentation found. Generate a presentation first.");
        }
    }
}
