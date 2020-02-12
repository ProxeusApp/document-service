package com.proxeus.xml.jtwig;

import com.proxeus.document.TemplateCompiler;
import com.proxeus.xml.Config;
import com.proxeus.xml.Element;
import com.proxeus.xml.Node;
import com.proxeus.xml.XmlTemplateHandler;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import sun.jvm.hotspot.interpreter.BytecodeStream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class XmlJTwigExtractorTest {

    @Test
    public void xmlTagsInIsland() {

        List<String> tests = Arrays.asList(
                //"xml_tags_in_island",
                //"template_with_code2",
                "template_with_code"
        );

        for (String test : tests) {
            XMLJTwigExtractor extractor = new XMLJTwigExtractor();

            InputStream input = XMLJTwigExtractor.class.getClassLoader().getResourceAsStream(test + ".xml");
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            // First, create a new XMLInputFactory
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            // Setup a new eventReader

            try {
                XMLEventReader eventReader = inputFactory.createXMLEventReader(input);
                extractor.extract(eventReader, output);
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println(output.toString());

            try {
                InputStream expected = XMLJTwigExtractor.class.getClassLoader().getResourceAsStream(test + "_fixed.xml");
                Assert.assertEquals(convert(expected, Charset.defaultCharset()), output.toString());
            } catch (IOException e) {

            }

        }


    }

    private String convert(InputStream inputStream, Charset charset) throws IOException {

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
