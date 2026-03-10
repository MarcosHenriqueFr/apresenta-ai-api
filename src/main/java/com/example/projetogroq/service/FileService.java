package com.example.projetogroq.service;

import com.example.projetogroq.dto.SlideStyle;
import com.example.projetogroq.dto.input.DownloadRequestDTO;
import com.example.projetogroq.dto.output.PresentationResponseDTO;
import com.example.projetogroq.dto.output.SlideDTO;
import com.example.projetogroq.utils.TemplateUtils;
import jakarta.servlet.http.HttpSession;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

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

    private byte[] createPptxFile(PresentationResponseDTO presentation, DownloadRequestDTO dto) throws IOException {
        try(XMLSlideShow ppt = getRelatedTemplate(dto)){

            XSLFSlideMaster master = ppt.getSlideMasters().getFirst();

            createTitleSlide(presentation.title(), ppt, master);
            createSlidesBullets(presentation.slides(), ppt, master);
            createGratitudeSlide(ppt, master);

            try(ByteArrayOutputStream baos = new ByteArrayOutputStream()){
                ppt.write(baos);
                return baos.toByteArray();
            }
        }
    }

    private void createGratitudeSlide(XMLSlideShow ppt, XSLFSlideMaster master) {
        XSLFSlideLayout layoutGratitude = TemplateUtils.getLayoutGratitude(master);
        XSLFSlide gratitudeSlide = ppt.createSlide(layoutGratitude);

        XSLFTextShape gratitudeTitle = gratitudeSlide.getPlaceholder(0);
        gratitudeTitle.setText("Muito obrigado pela sua atenção.");
    }

    private void createTitleSlide(String titleText, XMLSlideShow ppt, XSLFSlideMaster master) {
        XSLFSlideLayout layoutTitle = TemplateUtils.getLayoutTitle(master);

        XSLFSlide titleSlide = ppt.createSlide(layoutTitle);

        XSLFTextShape title = titleSlide.getPlaceholder(0);
        title.setText(titleText);

        XSLFTextShape subtitle = titleSlide.getPlaceholder(1);
        subtitle.setText("");
    }

    private void createSlidesBullets(List<SlideDTO> slides, XMLSlideShow ppt, XSLFSlideMaster master) {
        XSLFSlideLayout layoutContent = TemplateUtils.getLayoutTitleContent(master);

        XSLFSlide slide = null;
        XSLFTextShape title = null;
        XSLFTextShape content = null;

        for (SlideDTO slideDTO : slides) {
            slide = ppt.createSlide(layoutContent);
            title = slide.getPlaceholder(0);
            content = slide.getPlaceholder(1);

            title.setText(slideDTO.title());
            content.clearText();

            // Novos bullets precisam ser criados a cada slide
            for (String bulletPoint : slideDTO.bullets()) {
                XSLFTextParagraph bullet = content.addNewTextParagraph();
                bullet.setBullet(true);

                XSLFTextRun bulletText = bullet.addNewTextRun();
                bulletText.setText(bulletPoint);
            }
        }
    }

    private XMLSlideShow getRelatedTemplate(DownloadRequestDTO dto) throws IOException {
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
