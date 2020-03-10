package com.proxeus.document.odt.img;

import com.proxeus.xml.processor.XMLEventProcessor;
import com.proxeus.xml.template.TemplateVarParser;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageVarProcessor implements XMLEventProcessor {
    private final static String NAME_SPACE = "urn:oasis:names:tc:opendocument:xmlns:drawing:1.0";
    private final static Pattern imageOptionsRegex = Pattern.compile("(.*)\\[([^\\[\\]]*)\\]");

    private TemplateVarParser varParser;

    public ImageVarProcessor(TemplateVarParser varParser) {
        this.varParser = varParser;
    }

    @Override
    public void process(XMLEventReader reader, XMLEventWriter writer) throws XMLStreamException, IllegalStateException {
        while (reader.hasNext()) {
            XMLEvent e = reader.nextEvent();
            writer.add(e);

            if (!e.isStartElement()) {
                continue;
            }

            StartElement s = e.asStartElement();
            if (!s.getName().equals(new QName(NAME_SPACE, "frame"))) {
                continue;
            }
            Attribute attribute = s.getAttributeByName(new QName(NAME_SPACE, "name"));
            if (attribute == null) {
                continue;
            }

            String varWithOptions = attribute.getValue().trim();
            if (!(varWithOptions.startsWith("{{") && varWithOptions.endsWith("}}"))) {
                //continue as there is no valid var expression and therefore nothing for us to do on this image tag
                continue;
            }

            Matcher alignMatcher = imageOptionsRegex.matcher(varWithOptions);
            if (!alignMatcher.find()) {
                continue;
            }
            varWithOptions = alignMatcher.group(1).trim();

            varParser.parse(varWithOptions);
        }
    }
}
