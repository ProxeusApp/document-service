package com.proxeus.xml.template;

import com.proxeus.xml.processor.XMLEventProcessor;
import com.proxeus.xml.template.jtwig.JTwigParser;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;


@RunWith(Parameterized.class)
public class CleanEmptyElementProcessorTest {

    private static String TEXT = "urn:oasis:names:tc:opendocument:xmlns:text:1.0";
    private static QName TEXT_SPAN = new QName(TEXT, "span");
    private static QName TEXT_P = new QName(TEXT, "p");

    private static List<QName> EMPTY_XML_ELEMENT_TO_REMOVE = Arrays.asList(TEXT_SPAN, TEXT_P);

    private String test;

    public CleanEmptyElementProcessorTest(String test) {
        this.test = test;
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<? extends Object> tests() {
        return Arrays.asList(
                "template_with_code2_fixed.xml:template_with_code2_fixed.xml",
                "content_fixed.xml:content_fixed_cleaned.xml",
                "empty_element.xml:empty_element_cleaned.xml"
        );
    }

    @Test
    public void test() {

        String[] filenames = test.split(":");

        XMLEventProcessor cleaner = new CleanEmptyElementProcessor(EMPTY_XML_ELEMENT_TO_REMOVE);

        InputStream input = getClass().getClassLoader().getResourceAsStream(filenames[0]);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // First, create a new XMLInputFactory
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        // Setup a new eventReader

        try {
            XMLEventReader reader = inputFactory.createXMLEventReader(input);
            XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(output);
            cleaner.process(reader, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            InputStream expected = getClass().getClassLoader().getResourceAsStream(filenames[1]);
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