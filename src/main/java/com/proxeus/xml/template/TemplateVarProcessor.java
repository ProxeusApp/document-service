package com.proxeus.xml.template;

import com.proxeus.xml.processor.XMLEventProcessor;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;

public class TemplateVarProcessor implements XMLEventProcessor {
    private TemplateVarParser varParser;

    public TemplateVarProcessor(TemplateVarParser varParser) {
        this.varParser = varParser;
    }

    @Override
    public void process(XMLEventReader reader, XMLEventWriter writer) throws XMLStreamException, IllegalStateException {
        // TODO: Implement
    }
}
