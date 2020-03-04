package com.proxeus.xml.template;

import com.proxeus.document.TemplateCompiler;
import com.proxeus.xml.processor.XMLEventProcessor;
import com.proxeus.xml.template.jtwig.JTwigParser;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class DefaultTemplateHandlerTest {
    private String test;

    public DefaultTemplateHandlerTest(String test) {
        this.test = test;
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<? extends Object> tests() {
        return Arrays.asList(
                "xml_tags_in_island",
                "template_with_code2",
                "if_statement",
                "input1",
                "xml_element_spanning_template_blocks",
                "content");
    }

    @Test
    public void test() {

        try {
            XMLEventProcessor extractor = new TemplateExtractor(new JTwigParser());

            InputStream input = JTwigParser.class.getClassLoader().getResourceAsStream(test + ".xml");
            DefaultTemplateHandler handler = new DefaultTemplateHandler(extractor, new NoOpTemplateRenderer());
            handler.process(input);
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            handler.render(output, null);

            InputStream expected = JTwigParser.class.getClassLoader().getResourceAsStream(test + "_fixed.xml");
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
