package com.proxeus.xml.template;

import com.proxeus.xml.processor.XMLEventProcessor;
import com.proxeus.xml.processor.XMLEventProcessorChain;
import com.proxeus.xml.template.jtwig.JTwigParser;
import com.proxeus.xml.template.jtwig.JTwigVarParser;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class TemplateVarProcessorTest {


    @Test
    public void process() {

        List<String> codes = new ArrayList<>();

        codes.add("{{easy}}                                                           ".trim());
        codes.add("{% set easy=\"Proxeus makes document templating easier\" %}        ".trim());
        codes.add("{{input.ImageFile1}}                                               ".trim());
        codes.add("{{input.Var1*input.Var2}}                                          ".trim());
        codes.add("{{input.Var1*input.Var2}}                                          ".trim());
        codes.add("{{input.Var3/input.Var2}}                                          ".trim());
        codes.add("{{input.Var3/input.Var2}}                                          ".trim());
        codes.add("{{input.Var4}}                                                     ".trim());
        codes.add("{{(input.Var1>8)?\"Var is higher than 8\":\"Var is lower than 8\"}}".trim());
        codes.add("{{default(undefinedVariable,'World')}}                             ".trim());
        codes.add("{%if (condition)%}                                                 ".trim());
        codes.add("{%elseif (condition2)%}                                            ".trim());
        codes.add("{{loop.index0}}                                                    ".trim());
        codes.add("{{loop.index}}                                                     ".trim());
        codes.add("{{item}}                                                           ".trim());
        codes.add("{% for item in [1,2,3] %}                                          ".trim());
        codes.add("{{total*1000|number_format(2, \".\", \"'\")}}                      ".trim());
        codes.add("{{loop.index0}}                                                    ".trim());
        codes.add("{{loop.index}}                                                     ".trim());
        codes.add("{{item}}                                                           ".trim());
        codes.add("{% for item in [1,2,3] %}                                          ".trim());
        codes.add("{{total*1000|number_format(2, \".\", \"'\")}}                      ".trim());
        codes.add("{{ (iB.printed)?\"again \":\"\" }}                                 ".trim());
        codes.add("{{ (iB.printed)?myVarInsideInlineCond:SecondVarInsideInlineCond }} ".trim());
        codes.add("{{ iB.Name }}                                                      ".trim());
        codes.add("{% block inlineBlock %}                                            ".trim());
        codes.add("{{iB.Name}}                                                        ".trim());
        codes.add("{% block paragraphBlock %}                                         ".trim());
        codes.add("{{firstname}}                                                      ".trim());
        codes.add("{{lastname}}                                                       ".trim());
        codes.add("{{age}}                                                            ".trim());
        codes.add("{{firstname}}                                                      ".trim());
        codes.add("{%if (age<25) %}                                                   ".trim());
        codes.add("{%elseif (age>25 and age<32) %}                                    ".trim());
        codes.add("{%elseif (age>25 and age<32) %}                                    ".trim());
        codes.add("{% macro person (firstname, lastname, age) %}                      ".trim());
        codes.add("{% macro person (firstname, lastname, age) %}                      ".trim());
        codes.add("{% macro person (firstname, lastname, age) %}                      ".trim());
        codes.add("{% set input={Var1:10,Var2:25,Var3:1000,Var4:\"Text Value\"} %}    ".trim());
        codes.add("{% set total=0 %}                                                  ".trim());
        codes.add("{% set total=item+total %}                                         ".trim());
        codes.add("{% set total=item+total %}                                         ".trim());
        codes.add("{% set total=item+total %}                                         ".trim());
        codes.add("{% set total=0 %}                                                  ".trim());
        codes.add("{% set total=item+total %}                                         ".trim());
        codes.add("{% set total=item+total %}                                         ".trim());
        codes.add("{% set total=item+total %}                                         ".trim());
        codes.add("{% set iB={\"printed\":false,\"Name\":\"Steve\"} %}                ".trim());
        codes.add("{% set iB={\"Name\":\"John\"} %}                                   ".trim());
        codes.add("{{input.ImageFile1}}                                               ".trim());
        codes.add("{{loop.index0}}                                                    ".trim());
        codes.add("{{loop.index}}                                                     ".trim());
        codes.add("{{item}}                                                           ".trim());
        codes.add("{% for item in [1,2,3] %}                                          ".trim());
        codes.add("{{total*1000|number_format(2, \".\", \"'\")}}                      ".trim());
        codes.add("{% set total=0 %}                                                  ".trim());
        codes.add("{% set total=item+total %}                                         ".trim());
        codes.add("{% set total=item+total %}                                         ".trim());
        codes.add("{% set total=item+total %}                                         ".trim());

        StringBuffer sb = new StringBuffer();
        sb.append("<root>\n");
        for (String code : codes) {

            sb.append("<span>\n");
            sb.append(StringEscapeUtils.escapeXml11(code));
            sb.append("\n</span>\n");
        }
        sb.append("\n</root>");


        TemplateVarParser parser = new JTwigVarParser(null);
        XMLEventProcessor processor = new XMLEventProcessorChain(
                new TemplateExtractor(new JTwigParser()),
                // TemplateVarProcessor assumes that each code and content island are in their own characters event
                new TemplateVarProcessor(parser)
        );

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();

        try {
            XMLEventReader reader = inputFactory.createXMLEventReader(new ByteArrayInputStream(sb.toString().getBytes(Charset.defaultCharset())));
            XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(output);
            processor.process(reader, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Assert.assertEquals("[age, condition, condition2, easy, firstname, iB, iB.Name, iB.printed, inlineBlock, input, input.ImageFile1, input.Var1, input.Var2, input.Var3, input.Var4, item, lastname, loop.index, loop.index0, myVarInsideInlineCond, paragraphBlock, SecondVarInsideInlineCond, total, undefinedVariable]", parser.getVars().toString());
    }
}