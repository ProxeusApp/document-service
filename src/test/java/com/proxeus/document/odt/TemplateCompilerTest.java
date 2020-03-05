package com.proxeus.document.odt;

import com.proxeus.Application;
import com.proxeus.Config;
import com.proxeus.document.FileResult;
import com.proxeus.document.TemplateCompiler;
import com.proxeus.document.TemplateFormatter;
import com.proxeus.office.libre.exe.Extension;
import com.proxeus.util.zip.EntryFileFilter;
import com.proxeus.util.zip.Zip;
import com.proxeus.xml.template.jtwig.JTwigTemplateHandlerFactory;
import com.proxeus.xml.template.jtwig.JTwigTemplateVarParserFactory;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Test the compiler up to formatting.  Generate the ODT file whose content is then compared with the expected result.
 */
public class TemplateCompilerTest {

    @Test
    public void testCompile() throws Exception {

        Config config = Application.init();
        TemplateCompiler templateCompiler = new TemplateCompiler(config.getTmpFolder(), new TestTemplateFormatter(), new JTwigTemplateHandlerFactory(), new JTwigTemplateVarParserFactory());

        InputStream inputStream = new ByteArrayInputStream(createZip());

        FileResult result = templateCompiler.compile(inputStream, "pdf", false);
        System.out.printf("DEBUG %s\n", result);

        OutputStream content= new ByteArrayOutputStream();
        Zip.extract(result.target, new EntryFileFilter() {
            public void next(ZipEntry entry, ZipFile zf) throws Exception {
                if (entry.getName().startsWith("content.xml")) {
                    IOUtils.copy(zf.getInputStream(entry), content);
                }
            }
        });
        InputStream expected = getClass().getClassLoader().getResourceAsStream("content_noif_fixed_cleaned.xml");
        Assert.assertEquals(convert(expected, Charset.defaultCharset()), content.toString());




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

    private class TestTemplateFormatter implements TemplateFormatter {

        @Override
        public String Convert(File src, File dst, String format, boolean restart) throws Exception {
            Assert.assertEquals(false, restart);
            Assert.assertEquals("pdf", format);

            Assert.assertEquals(src.getParentFile(), dst.getParentFile());
            Assert.assertEquals("tmpl.odt", src.getName());
            Assert.assertEquals("final", dst.getName());
            Files.copy(src.toPath(), dst.toPath());
            return "test";
        }

        @Override
        public Extension getExtension(String os) {
            return null;
        }
    }
}
