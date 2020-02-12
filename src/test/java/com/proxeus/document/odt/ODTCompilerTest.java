package com.proxeus.document.odt;

/*
 *
 *
 
 public class LibreConfig {
     * "/opt/libreoffice5.4/program" "C:/Program Files/LibreOffice 5/program" "/usr/lib/libreoffice/program"
    public String librepath = "/usr/lib/libreoffice/program";
     * min executables ready to be ready. An executable is mainly needed to convert to PDF. It is recommended to use one exe for a request at the time.
    public int min = 8;
     * max capacity of executable running. The next request will be on hold until one is freed or until request timeout.
    public int max = 40;

    public int highLoad = 60;
}
 *
 * package com.proxeus.document;

import com.proxeus.compiler.jtwig.MyJTwigCompiler;
import com.proxeus.document.docx.DOCXCompiler;
import com.proxeus.document.odt.ODTCompiler;
import com.proxeus.error.BadRequestException;
import com.proxeus.office.libre.LibreOfficeAssistant;
import com.proxeus.office.microsoft.MicrosoftOfficeAssistant;
import com.proxeus.util.Json;
import com.proxeus.util.zip.EntryFilter;
import com.proxeus.util.zip.Zip;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;

import static com.proxeus.document.TemplateType.DOCX;
import static com.proxeus.document.TemplateType.ODT;


public class TemplateCompiler {
    private ODTCompiler odtCompiler;
    private DOCXCompiler docxCompiler;
    private MyJTwigCompiler compiler;

    public TemplateCompiler(String cacheFolder, LibreOfficeAssistant libreOfficeAssistant) throws Exception{
        compiler = new MyJTwigCompiler();
        odtCompiler = new ODTCompiler(cacheFolder, compiler, libreOfficeAssistant);
        docxCompiler = new DOCXCompiler(cacheFolder, compiler, new MicrosoftOfficeAssistant());
    }

    public FileResult compile(InputStream zipStream, String format, boolean embedError) throws Exception{
        Template template = provideTemplateFromZIP(zipStream, format);
        template.embedError = embedError;
        return getCompiler(template).Compile(template);
    }

*/

public class ODTCompilerTest {
/*
    private ODTCompiler odtCompiler;

    @Test
    public void testCompile() throws Exception {
        MyJTwigCompiler compiler = new MyJTwigCompiler();

        LibreConfig config = new LibreConfig();
        config.librepath = "/Applications/LibreOffice.app/Contents/MacOS/soffice";
        LibreOfficeAssistant libreOfficeAssistant = new LibreOfficeAssistant(Config.by(LibreConfig.class));

        odtCompiler = new ODTCompiler("/tmp", compiler, libreOfficeAssistant);
        Template template = provideTemplateFromZIP(zipStream, format);

        FileResult result = odtCompiler.Compile(template);

    }
    */
}
