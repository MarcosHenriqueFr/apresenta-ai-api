package com.example.projetogroq.service;

import com.example.projetogroq.dto.enums.SlideStyle;
import com.example.projetogroq.dto.input.DownloadRequestDTO;
import com.example.projetogroq.dto.output.PresentationResponseDTO;
import com.example.projetogroq.dto.output.SlideDTO;
import com.example.projetogroq.exception.custom.IllegalPresentationStateException;
import com.example.projetogroq.exception.custom.InvalidFileOperationException;
import com.example.projetogroq.utils.TemplateUtils;
import jakarta.servlet.http.HttpSession;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class FileService {

    private static final Integer MAX_CHARS_PER_PDF = 5000;
    private static final int MAX_PDF_FILES = 3;
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    private final SessionService sessionService;
    private final Executor executor;

    public FileService(SessionService sessionService, Executor executor) {
        this.sessionService = sessionService;
        this.executor = executor;
    }

    public byte[] downloadPptxFile(HttpSession session, DownloadRequestDTO dto) throws IOException {
        PresentationResponseDTO presentation = getPresentationData(session);

        return createPptxFile(presentation, dto);
    }

    /**
     * Agrupa toda a lógica para a criação do arquivo, se baseando principalmente nas informações guardadas em sessão
     *
     * @param presentation Contém a resposta da API externa na sessão
     * @param dto          Contém as informações de estilo
     * @return O arquivo definido de acordo com o template
     * @throws IOException Caso o arquivo de template não seja encontrado no servidor
     */
    private byte[] createPptxFile(PresentationResponseDTO presentation, DownloadRequestDTO dto) throws IOException {
        try (XMLSlideShow ppt = getRelatedTemplate(dto)) {

            XSLFSlideMaster master = ppt.getSlideMasters().getFirst();

            createTitleSlide(presentation.title(), ppt, master);
            createSlidesBullets(presentation.slides(), ppt, master);
            createGratitudeSlide(ppt, master);

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
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

    /**
     * Age sobre a resposta da API externa para a formatação desse DTO em slides dentro de um arquivo
     * .pptx
     *
     * @param slides Provenientes de {@link PresentationResponseDTO}
     * @param ppt    Objeto que agrupa todos os componentes do slide, como definido no template
     * @param master Responsável pela visão dos layouts do template
     */
    private void createSlidesBullets(List<SlideDTO> slides, XMLSlideShow ppt, XSLFSlideMaster master) {
        XSLFSlideLayout layoutContent = TemplateUtils.getLayoutTitleContent(master);

        XSLFSlide slide;
        XSLFTextShape title;
        XSLFTextShape content;

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

        SlideStyle desiredStyle = SlideStyle.valueOf(dto.style());

        if (desiredStyle == SlideStyle.ACADEMIC) {
            return TemplateUtils.loadTemplateAcademic();
        } else if (desiredStyle == SlideStyle.CREATIVE) {
            return TemplateUtils.loadTemplateCreative();
        }

        return TemplateUtils.loadTemplateBasic();
    }

    /**
     * Abstrai a lógica de existência da {@link HttpSession} e do {@link PresentationResponseDTO}.
     * Garantindo que ambos tenham sido instânciados através do {@link SessionService}.
     *
     * @param session Recebida da request do client
     * @return Um presentation DTO válido
     * @throws IllegalPresentationStateException Caso uma sessão ou apresentação não exista.
     */
    private PresentationResponseDTO getPresentationData(HttpSession session) {
        sessionService.checkSessionExistence(session);

        PresentationResponseDTO presentationDTO = sessionService.getPresentationData(session);
        checkPresentationExistence(presentationDTO);

        return presentationDTO;
    }

    private void checkPresentationExistence(PresentationResponseDTO presentation) {
        if (presentation == null) {
            throw new IllegalPresentationStateException("No presentation found. Generate a presentation first.");
        }
    }

    public String getContextFromFiles(List<MultipartFile> pdfs, boolean available) {
        if (!available) return "";

        StopWatch watch = new StopWatch();
        watch.start();

        List<CompletableFuture<String>> futures = extractPdfTextsAsync(pdfs);
        String result = joinPdfTexts(futures);

        watch.stop();
        logger.debug("PDF extraction completed in {}ms", watch.getTotalTimeMillis());

        return result;
    }

    /**
     * Lê os bytes de cada PDF e despacha a extração de texto de forma assíncrona.
     * A leitura dos bytes ocorre na thread chamadora para evitar acesso concorrente ao MultipartFile.
     *
     * @param pdfs Arquivos enviados pelo client.
     * @return Lista de futures com o texto extraído de cada PDF.
     */
    private List<CompletableFuture<String>> extractPdfTextsAsync(List<MultipartFile> pdfs) {
        return pdfs.stream()
                .map(file -> {
                    try {
                        byte[] bytes = file.getBytes();
                        return CompletableFuture.supplyAsync(() -> extractTextFromBytes(bytes), executor);
                    } catch (IOException e) {
                        throw new InvalidFileOperationException("Failed to read bytes from uploaded PDF.", e);
                    }
                })
                .toList();
    }

    /**
     * Aguarda a conclusão de todos os futures e concatena os textos em um único contexto.
     *
     * @param futures Futures com texto extraído de cada PDF.
     * @return Texto unificado para uso como contexto no prompt.
     */
    private String joinPdfTexts(List<CompletableFuture<String>> futures) {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        StringBuilder sb = new StringBuilder("Use essas informações como apoio de dados reais.\n");
        futures.stream()
                .map(CompletableFuture::join)
                .forEach(sb::append);

        return sb.toString();
    }

    String extractTextFromBytes(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            String fullText = new PDFTextStripper().getText(document);

            if (fullText.length() > MAX_CHARS_PER_PDF) {
                return fullText.substring(0, MAX_CHARS_PER_PDF) + "\n[TRUNCATED TEXT]";
            }

            return fullText;
        } catch (IOException e) {
            throw new InvalidFileOperationException("Couldn't read text from PDF.", e);
        }
    }

    boolean checkPdfAvailability(List<MultipartFile> pdfs) {
        if (pdfs == null) {
            return false;
        }

        if (pdfs.size() > MAX_PDF_FILES) {
            throw new IllegalArgumentException("The file limit has been exceeded.");
        }

        return true;
    }
}
