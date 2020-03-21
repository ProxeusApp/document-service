package com.proxeus.document.odt;

import com.proxeus.document.AssetFile;
import com.proxeus.xml.processor.XMLEventProcessor;
import com.proxeus.xml.template.DefaultTemplateHandler;
import com.proxeus.xml.template.NoOpTemplateRenderer;
import com.proxeus.xml.template.jtwig.JTwigParser;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class ODTManifestProcessorTest {
 private String test;

    public ODTManifestProcessorTest (String test) {
        this.test = test;
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<? extends Object> tests() {
        return Arrays.asList(
                "manifest"
        );
    }

    @Test
    public void test() {
        try {

            Queue<AssetFile> assetFiles = new LinkedList<>();
            AssetFile assetFile = new AssetFile();
            assetFile.orgZipPath = "Pictures/10000000000000850000006377498356141931B6.jpg";
            assetFile.newZipPath = "foobar.jpg";

            assetFiles.offer(assetFile);
            XMLEventProcessor processor = new ODTManifestProcessor(assetFiles);

            InputStream input = getClass().getClassLoader().getResourceAsStream(test + ".xml");
            DefaultTemplateHandler handler = new DefaultTemplateHandler(processor, new NoOpTemplateRenderer());
            handler.process(input);
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            handler.render(output, null);

            InputStream expected = JTwigParser.class.getClassLoader().getResourceAsStream(test + "_processed.xml");
            Assert.assertEquals(convert(expected, Charset.defaultCharset()), output.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String convert(InputStream inputStream, Charset charset) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}