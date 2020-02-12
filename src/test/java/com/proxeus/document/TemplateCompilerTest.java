package com.proxeus.document;

import com.proxeus.document.FileResult;
import com.proxeus.document.Template;
import com.proxeus.document.TemplateCompiler;
import com.proxeus.compiler.jtwig.MyJTwigCompiler;
import com.proxeus.document.odt.ODTCompiler;
import com.proxeus.office.libre.LibreConfig;
import com.proxeus.office.libre.LibreOfficeAssistant;
import com.proxeus.Application;
import com.proxeus.Config;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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


            post("/compile", (request, response) -> {
            try {
                StopWatch sw = StopWatch.createStarted();
                FileResult result = templateCompiler.compile(request.raw().getInputStream(), request.queryParams("format"), request.queryMap().hasKey("error"));
                response.header("Content-Type", result.contentType);
                response.header("Content-Length", "" + result.target.length());
                try {
                    streamAndClose(new FileInputStream(result.target), response.raw().getOutputStream());
                } finally {
                    result.release();
                }
                System.out.println("request took: " + sw.getTime(TimeUnit.MILLISECONDS));
            } catch(EofException | MultipartStream.MalformedStreamException eof){
                try{
                    response.raw().getOutputStream().close();
                }catch (Exception idc){}
            } catch (CompilationException e) {
                error(422, response, e);
            } catch (BadRequestException e) {
                error(HttpURLConnection.HTTP_BAD_REQUEST, response, e);
            } catch (NotImplementedException e) {
                error(HttpURLConnection.HTTP_NOT_IMPLEMENTED, response, e);
            } catch (UnavailableException e) {
                error(HttpURLConnection.HTTP_UNAVAILABLE, response, e);
            } catch (Exception e) {
                error(HttpURLConnection.HTTP_INTERNAL_ERROR, response, e);
            }
            return 0;
        });
 
*/

public class TemplateCompilerTest {
    private LibreOfficeAssistant libreOfficeAssistant;
    private TemplateCompiler templateCompiler;

    @Test
    public void testCompile() throws Exception {
        
        Config config = Application.init();
        libreOfficeAssistant = new LibreOfficeAssistant(Config.by(LibreConfig.class));
        templateCompiler = new TemplateCompiler(config.getTmpFolder(), libreOfficeAssistant);

        InputStream inputStream = new ByteArrayInputStream(createZip());

        FileResult result = templateCompiler.compile(inputStream, "pdf", true);
        System.out.println(result.target.getAbsolutePath());
    }

    private byte[] createZip() throws Exception {
        List<String> srcFiles = Arrays.asList("simple.odt", "simple.json");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(os);
        for (String srcFile : srcFiles) {
            System.out.println(srcFile);
            InputStream fis = getClass().getClassLoader().getResourceAsStream(srcFile);
            ZipEntry zipEntry = new ZipEntry(srcFile);
            zipOut.putNextEntry(zipEntry);
 
            byte[] bytes = new byte[1024];
            int length;
            while((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            fis.close();
        }
        zipOut.close();

        return os.toByteArray(); 
    }
}
