package com.proxeus.document;

import com.proxeus.office.libre.LibreConfig;
import com.proxeus.office.libre.LibreOfficeAssistant;
import com.proxeus.Application;
import com.proxeus.Config;

import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Ignore
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
        List<String> srcFiles = Arrays.asList("old/simple.odt", "old/simple.json");
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
