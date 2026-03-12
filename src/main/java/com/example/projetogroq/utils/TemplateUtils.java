package com.example.projetogroq.utils;

import com.example.projetogroq.exception.PresentationTemplateNotFoundIOException;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;

import java.io.IOException;
import java.io.InputStream;

/**
 * Classe de acesso aos templates e layout criados nele.
 * Faz o acesso de nível mais baixo aos templates dentro dos recursos do servidor.
 */
public class TemplateUtils {

    private static final String BASIC_PATH = "/templates/ppt/presentation_template1.pptx";
    private static final String ACADEMIC_PATH = "/templates/ppt/presentation_template2.pptx";
    private static final String CREATIVE_PATH = "/templates/ppt/presentation_template3.pptx";

    private static final String TITLE = "TitleSlide";
    private static final String TITLE_CONTENT = "TitleContent";
    private static final String GRATITUDE = "Gratitude";

    public static XMLSlideShow loadTemplate(String path) throws IOException {
        InputStream is = TemplateUtils.class.getResourceAsStream(path);

        if(is == null){
            throw new PresentationTemplateNotFoundIOException("Template was not found: " + path);
        }

        return new XMLSlideShow(is);
    }

    public static XMLSlideShow loadTemplateBasic() throws IOException {
        return loadTemplate(BASIC_PATH);
    }

    public static XMLSlideShow loadTemplateAcademic() throws IOException {
        return loadTemplate(ACADEMIC_PATH);
    }

    public static XMLSlideShow loadTemplateCreative() throws IOException {
        return loadTemplate(CREATIVE_PATH);
    }

    public static XSLFSlideLayout getLayoutTitle(XSLFSlideMaster master){
        return master.getLayout(TITLE);
    }

    public static XSLFSlideLayout getLayoutTitleContent(XSLFSlideMaster master){
        return master.getLayout(TITLE_CONTENT);
    }

    public static XSLFSlideLayout getLayoutGratitude(XSLFSlideMaster master){
        return master.getLayout(GRATITUDE);
    }
}
