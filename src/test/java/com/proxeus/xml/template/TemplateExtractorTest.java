package com.proxeus.xml.template;

import com.proxeus.xml.processor.XMLEventProcessor;
import com.proxeus.xml.template.jtwig.JTwigParser;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import javax.xml.stream.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class TemplateExtractorTest {

    private String test;

    public TemplateExtractorTest(String test) {
        this.test = test;
    }

    @Parameters(name = "{index}: {0}")
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


        XMLEventProcessor extractor = new TemplateExtractor(new JTwigParser());

        InputStream input = JTwigParser.class.getClassLoader().getResourceAsStream(test + ".xml");
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // First, create a new XMLInputFactory
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        // Setup a new eventReader

        try {
            XMLEventReader reader = inputFactory.createXMLEventReader(input);
            XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(output);
            extractor.process(reader, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(output.toString());

        try {
            InputStream expected = JTwigParser.class.getClassLoader().getResourceAsStream(test + "_fixed.xml");
            Assert.assertEquals(convert(expected, Charset.defaultCharset()), output.toString());
        } catch (IOException e) {

        }

    }

    private String convert(InputStream inputStream, Charset charset) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
