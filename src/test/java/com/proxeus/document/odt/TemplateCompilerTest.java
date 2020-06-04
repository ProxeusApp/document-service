package com.proxeus.document.odt;

import com.proxeus.Application;
import com.proxeus.Config;
import com.proxeus.document.FileResult;
import com.proxeus.document.Template;
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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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
@RunWith(Parameterized.class)
public class TemplateCompilerTest {

    private String test;

    public TemplateCompilerTest(String test) {
        this.test = test;
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<? extends Object> tests() {
        return Arrays.asList(
                "simple_noif_template.odt:simple.json:content_noif_fixed_cleaned.xml",
                "crypto_asset_report.odt:crypto_asset_report.json:crypto_asset_report_fixed_cleaned.xml",
                "proof_of_existence.odt:proof_of_existence.json:proof_of_existence_fixed_cleaned.xml"
        );
    }

    @Test
    public void testCompile() throws Exception {

        String[] filenames = test.split(":");

        Config config = Application.init();
        TemplateCompiler templateCompiler = new TemplateCompiler(config.getTmpFolder(), new TestTemplateFormatter(), new JTwigTemplateHandlerFactory(), new JTwigTemplateVarParserFactory());

        InputStream inputStream = new ByteArrayInputStream(createZip(filenames[0], filenames[1]));

        FileResult result = templateCompiler.compile(Template.fromZip(inputStream, "pdf"), false);

        OutputStream content = new ByteArrayOutputStream();
        Zip.extract(result.target, new EntryFileFilter() {
            public void next(ZipEntry entry, ZipFile zf) throws Exception {
                if (entry.getName().startsWith("content.xml")) {
                    IOUtils.copy(zf.getInputStream(entry), content);
                }
            }
        });

        BufferedWriter writer = new BufferedWriter(new FileWriter("/tmp/" + filenames[2]));
        writer.write(content.toString());
        writer.close();

        InputStream expected = getClass().getClassLoader().getResourceAsStream(filenames[2]);
        Assert.assertEquals(convert(expected, Charset.defaultCharset()), content.toString());
    }


    private byte[] createZip(String odt, String json) throws Exception {
        List<String> srcFiles = Arrays.asList(odt, json);
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
    }
}
