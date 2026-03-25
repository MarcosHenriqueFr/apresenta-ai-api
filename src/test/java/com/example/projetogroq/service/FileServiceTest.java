package com.example.projetogroq.service;

import com.example.projetogroq.dto.input.DownloadRequestDTO;
import com.example.projetogroq.exception.custom.IllegalPresentationStateException;
import com.example.projetogroq.exception.custom.IllegalSessionStateException;
import jakarta.servlet.http.HttpSession;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileServiceTest {

    @Mock
    private SessionService sessionService;

    @Mock
    private HttpSession session;

    private AutoCloseable closeable;
    private FileService fileService;

    @BeforeEach
    void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        FileService real = new FileService(sessionService, Runnable::run);
        fileService = spy(real);
    }

    @AfterEach
    void closeMocks() throws Exception {
        closeable.close();
    }

    @Test
    @DisplayName("Should throw IllegalSessionStateException when session is null on download")
    void downloadPptxFileThrowsWhenSessionIsNull() {
        DownloadRequestDTO dto = new DownloadRequestDTO("BASIC");
        doThrow(new IllegalSessionStateException("No active session."))
                .when(sessionService).checkSessionExistence(null);

        assertThrows(IllegalSessionStateException.class, () ->
                fileService.downloadPptxFile(null, dto)
        );
    }

    @Test
    @DisplayName("Should throw IllegalPresentationStateException when no presentation exists in session")
    void downloadPptxFileThrowsWhenPresentationIsNull() {
        DownloadRequestDTO dto = new DownloadRequestDTO("BASIC");
        when(sessionService.getPresentationData(session)).thenReturn(null);

        assertThrows(IllegalPresentationStateException.class, () ->
                fileService.downloadPptxFile(session, dto)
        );
    }

    @Test
    @DisplayName("Should return empty string when pdfs are not available")
    void getContextFromFilesReturnsEmptyWhenNotAvailable() {
        String result = fileService.getContextFromFiles(List.of(), false);

        assertEquals("", result);
    }

    @Test
    @DisplayName("Should return context string containing extracted pdf text when available")
    void getContextFromFilesReturnsContextWhenAvailable() throws IOException {
        MultipartFile pdf = mock(MultipartFile.class);
        byte[] bytes = {1, 2, 3};
        when(pdf.getBytes()).thenReturn(bytes);
        doReturn("extracted text").when(fileService).extractTextFromBytes(bytes);

        String result = fileService.getContextFromFiles(List.of(pdf), true);

        assertTrue(result.contains("extracted text"));
    }

    @Test
    @DisplayName("Should truncate text and append marker when content exceeds max characters")
    void extractTextFromBytesReturnsTruncatedText() throws IOException {
        byte[] pdfBytes = buildPdfWithLongText(6000);

        String result = fileService.extractTextFromBytes(pdfBytes);

        assertTrue(result.endsWith("[TRUNCATED TEXT]"));
    }

    @Test
    @DisplayName("Should return false when pdf list is null")
    void checkPdfAvailabilityReturnsFalseWhenNull() {
        boolean result = fileService.checkPdfAvailability(null);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when pdf count exceeds limit of 3")
    void checkPdfAvailabilityThrowsWhenLimitExceeded() {
        List<MultipartFile> pdfs = List.of(
                mock(MultipartFile.class),
                mock(MultipartFile.class),
                mock(MultipartFile.class),
                mock(MultipartFile.class)
        );

        assertThrows(IllegalArgumentException.class, () ->
                fileService.checkPdfAvailability(pdfs)
        );
    }

    @Test
    @DisplayName("Should return true for a valid non-empty pdf list within the limit")
    void checkPdfAvailabilityReturnsTrueForValidList() {
        List<MultipartFile> pdfs = List.of(mock(MultipartFile.class));

        boolean result = fileService.checkPdfAvailability(pdfs);

        assertTrue(result);
    }

    private byte[] buildPdfWithLongText(int targetChars) throws IOException {
        String line = "A".repeat(500);
        int pages = (targetChars / line.length()) + 1;

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    cs.newLineAtOffset(10, 700);
                    cs.showText(line);
                    cs.endText();
                }
            }
            document.save(baos);
            return baos.toByteArray();
        }
    }
}