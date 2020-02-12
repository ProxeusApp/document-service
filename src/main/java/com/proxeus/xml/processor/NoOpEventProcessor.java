package com.proxeus.xml.processor;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;

public class NoOpEventProcessor implements XMLEventProcessor {
    @Override
    public void process(XMLEventReader reader, XMLEventWriter writer) throws XMLStreamException, IllegalStateException {
        if(reader == null || writer == null){
            return;
        }
        while(reader.hasNext()){
            writer.add(reader.nextEvent());
        }
    }
}
