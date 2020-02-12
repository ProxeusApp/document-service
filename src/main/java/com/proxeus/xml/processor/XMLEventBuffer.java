package com.proxeus.xml.processor;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.util.Iterator;
import java.util.LinkedList;

public class XMLEventBuffer implements XMLEventReadWriter {
    private LinkedList<XMLEvent> events = new LinkedList<>();
    private XMLEventReader reader;

    public XMLEventBuffer() {

    }

    public XMLEventBuffer(XMLEventReader reader) {
        this.reader = reader;
    }

    public Iterator<XMLEvent> interator() {
        return events.iterator();
    }

    @Override
    public void flush() throws XMLStreamException {

    }

    @Override
    public void close() throws XMLStreamException {
        events = null;
    }

    // Writer
    @Override
    public void add(XMLEvent event) throws XMLStreamException {
        events.add(event);
    }

    @Override
    public void add(XMLEventReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            events.add(reader.nextEvent());
        }
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        // Not implemented
        return "";
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        // Not implemented
    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        // Not implemented
    }

    @Override
    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        // Not implemented
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        // Not implemented
        return null;
    }

    // Reader
    @Override
    public XMLEvent nextEvent() throws XMLStreamException {
        emptyReader();
        return events.removeFirst();
    }

    @Override
    public boolean hasNext() {
        emptyReader();
        return events.size() > 0;
    }

    @Override
    public Object next() {
        emptyReader();
        return events.removeFirst();
    }

    @Override
    public XMLEvent peek() throws XMLStreamException {
        return events.peekFirst();
    }

    @Override
    public String getElementText() throws XMLStreamException {
        // Not implemented
        return "";
    }

    @Override
    public XMLEvent nextTag() throws XMLStreamException {
        // Not implemented
        return null;
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        // Not implemented
        return null;
    }

    private void emptyReader() {
        if (this.reader != null) {
            while (reader.hasNext()) {
                try {
                    add(reader.nextEvent());
                } catch (XMLStreamException e) {
                    throw new RuntimeException(e);
                }
            }
            this.reader = null;
        }
    }
}
