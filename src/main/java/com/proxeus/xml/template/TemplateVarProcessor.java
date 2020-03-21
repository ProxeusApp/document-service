package com.proxeus.xml.template;

import com.proxeus.xml.processor.XMLEventProcessor;
import org.apache.log4j.Logger;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;

public class TemplateVarProcessor implements XMLEventProcessor {

    private Logger log = Logger.getLogger(this.getClass());
    private TemplateVarParser varParser;

    public TemplateVarProcessor(TemplateVarParser varParser) {
        this.varParser = varParser;
    }

    @Override
    public void process(XMLEventReader reader, XMLEventWriter writer) throws XMLStreamException, IllegalStateException {
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            writer.add(event);

            if(!event.isCharacters()){
                continue;
            }

            Characters c = event.asCharacters();
            varParser.parse(c.getData());
        }
    }
}
