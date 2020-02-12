package com.proxeus.xml.processor;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;

public interface XMLEventProcessor {
    @SuppressWarnings({"unchecked", "null"})
    void process(XMLEventReader reader, XMLEventWriter writer) throws XMLStreamException, IllegalStateException;
}
