package com.proxeus.xml.processor;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

public class XMLEventSink implements XMLEventWriter {
    @Override
    public void flush() throws XMLStreamException {

    }

    @Override
    public void close() throws XMLStreamException {

    }

    @Override
    public void add(XMLEvent event) throws XMLStreamException {

    }

    @Override
    public void add(XMLEventReader reader) throws XMLStreamException {

    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return null;
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {

    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {

    }

    @Override
    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {

    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return null;
    }
}
