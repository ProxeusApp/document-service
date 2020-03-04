package com.proxeus.xml.template;

import com.proxeus.xml.processor.XMLEventProcessor;
import org.apache.log4j.Logger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.*;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;


public class DefaultTemplateHandler implements TemplateHandler {

    private Logger log = Logger.getLogger(this.getClass());
    private TemplateXMLEventWriter events;
    private XMLEventProcessor processor;
    private TemplateRenderer renderer;

    public DefaultTemplateHandler(XMLEventProcessor processor, TemplateRenderer renderer) {
        log.trace("DEBUG NEW TEMPLATE HANDLER");
        this.events = new TemplateXMLEventWriter();
        this.processor = processor;
        this.renderer = renderer;
    }

    @Override
    public void process(InputStream input) throws Exception {
        // First, create a new XMLInputFactory
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        // Setup a new eventReader
        try {
            XMLEventReader reader = inputFactory.createXMLEventReader(input);
            processor.process(reader, events);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render(OutputStream output, Map<String, Object> data) throws Exception {
        log.trace(String.format("DEBUG TEMPLATE HANDLER TO OUTPUT STREAM %s\n", events.toString()));
        ByteArrayOutputStream xmlOutput = new ByteArrayOutputStream();

        XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(xmlOutput);
        Charset charset = UTF_8;
        Iterator<XMLEvent> it = events.interator();
        while (it.hasNext()) {
            XMLEvent e = it.next();
            if (e.isStartDocument()) {
                StartDocument sd = (StartDocument) e;
                charset = Charset.forName(sd.getCharacterEncodingScheme());
            }
            writer.add(e);
        }

        if (data == null){
            data = Collections.emptyMap();
        }
        InputStream input = new ByteArrayInputStream(xmlOutput.toByteArray());
        this.renderer.render(input, output, data, charset);
    }

    @Override
    public Charset getCharset() {
        return Charset.defaultCharset();
    }

    @Override
    public void toOutputStream(OutputStream output) throws IOException {

    }

    @Override
    public void free() {

    }

    private class TemplateXMLEventWriter implements XMLEventWriter {
        private LinkedList<XMLEvent> events = new LinkedList<>();

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
    }
}
