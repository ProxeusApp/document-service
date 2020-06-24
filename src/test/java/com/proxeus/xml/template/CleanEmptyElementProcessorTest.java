package com.proxeus.xml.template;

import com.proxeus.xml.processor.XMLEventProcessor;
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


@RunWith(Parameterized.class)
public class CleanEmptyElementProcessorTest {

    private static String TEXT = "urn:oasis:names:tc:opendocument:xmlns:text:1.0";
    private static QName TEXT_SPAN = new QName(TEXT, "span");
    private static QName TEXT_P = new QName(TEXT, "p");

    private static List<QName> ELEMENT_TO_REMOVE_IF_EMPTY = Arrays.asList(TEXT_SPAN);
    private static List<QName> ELEMENT_TO_REMOVE_IF_ONLY_WHITESPACE = Arrays.asList(TEXT_P);

    private String test;

    public CleanEmptyElementProcessorTest(String test) {
        this.test = test;
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<? extends Object> tests() {
        return Arrays.asList(
                "template_with_code2_fixed.xml:template_with_code2_fixed.xml",
                "content_fixed.xml:content_fixed_cleaned.xml",
                "empty_element.xml:empty_element_cleaned.xml",
                "paragraph_fixed.xml:paragraph_fixed_cleaned.xml",
                "small_paragraph_fixed.xml:small_paragraph_fixed_cleaned.xml"
        );
    }

    @Test
    public void test() throws Exception {

        String[] filenames = test.split(":");

        XMLEventProcessor cleaner = new CleanEmptyElementProcessor(ELEMENT_TO_REMOVE_IF_EMPTY, ELEMENT_TO_REMOVE_IF_ONLY_WHITESPACE);

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

        String result = output.toString();

        BufferedWriter writer = new BufferedWriter(new FileWriter("/tmp/" + filenames[1]));
        writer.write(result);
        writer.close();

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