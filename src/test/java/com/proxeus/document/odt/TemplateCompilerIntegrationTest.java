package com.proxeus.document.odt;

import com.proxeus.IntegrationTest;
import com.proxeus.document.FileResult;
import com.proxeus.document.TemplateCompiler;
import com.proxeus.office.libre.LibreConfig;
import com.proxeus.office.libre.LibreOfficeAssistant;
import com.proxeus.Application;
import com.proxeus.Config;

import com.proxeus.xml.template.jtwig.JTwigTemplateHandlerFactory;
import com.proxeus.xml.template.jtwig.JTwigTemplateVarParserFactory;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class TemplateCompilerIntegrationTest {

    @Test
    @Category(IntegrationTest.class) // Integration test because it call the actual LibreOffice to generate pdf.
    @Ignore
    public void testCompile() throws Exception {

        Config config = Application.init();
        LibreOfficeAssistant libreOfficeAssistant = new LibreOfficeAssistant(Config.by(LibreConfig.class));
        TemplateCompiler templateCompiler = new TemplateCompiler(config.getTmpFolder(), libreOfficeAssistant, new JTwigTemplateHandlerFactory(), new JTwigTemplateVarParserFactory());

        InputStream inputStream = new ByteArrayInputStream(createZip());

        FileResult result = templateCompiler.compile(inputStream, "pdf", false);
        System.out.printf("DEBUG %s\n", result);

        String pdf = new String(Files.readAllBytes(Paths.get(result.target.getAbsolutePath())), StandardCharsets.UTF_8);

        InputStream expected = getClass().getClassLoader().getResourceAsStream("simple_noif.pdf");
        Assert.assertEquals(cleanPdf(convert(expected, Charset.defaultCharset())),cleanPdf(pdf));
    }


    private byte[] createZip() throws Exception {
        List<String> srcFiles = Arrays.asList("simple_noif_template.odt", "simple.json");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(os);
        for (String srcFile : srcFiles) {
            System.out.println(srcFile);
            InputStream fis = getClass().getClassLoader().getResourceAsStream(srcFile);
            ZipEntry zipEntry = new ZipEntry(srcFile);
            zipOut.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            fis.close();
        }
        zipOut.close();

        return os.toByteArray();
    }

    private String convert(InputStream inputStream, Charset charset) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    private Pattern pat = Pattern.compile("(^<</Title.+?>>)|(^/ID.+?>>)", Pattern.MULTILINE | Pattern.DOTALL);
    private String cleanPdf(String input){
        return pat.matcher(input).replaceAll("");
    }
}
