package com.proxeus.xml.template;

import com.proxeus.xml.processor.XMLEventProcessor;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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

    private Logger log = LogManager.getLogger(this.getClass());
    private TemplateXMLEventWriter events;
    private XMLEventProcessor preProcessor;
    private XMLEventProcessor postProcessor;
    private TemplateRenderer renderer;

    private XMLEventFactory eventFactory = XMLEventFactory.newInstance();

    public DefaultTemplateHandler(XMLEventProcessor preProcessor, TemplateRenderer renderer, XMLEventProcessor postProcessor) {
        this.events = new TemplateXMLEventWriter();
        this.preProcessor = preProcessor;
        this.renderer = renderer;
        this.postProcessor = postProcessor;
    }

    @Override
    public void process(InputStream input) throws Exception {
        // First, create a new XMLInputFactory
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        // Setup a new eventReader
        try {
            XMLEventReader reader = inputFactory.createXMLEventReader(input);
            this.preProcessor.process(reader, events);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render(OutputStream output, Map<String, Object> data) throws Exception {
        ByteArrayOutputStream xmlOutput = new ByteArrayOutputStream();

        XMLOutputFactory outputXMLFactory = XMLOutputFactory.newInstance();
        outputXMLFactory.setProperty("escapeCharacters", false);

        XMLEventWriter writer = outputXMLFactory.createXMLEventWriter(xmlOutput);
        Charset charset = UTF_8;
        Iterator<XMLEvent> it = events.interator();
        while (it.hasNext()) {
            XMLEvent e = it.next();
            if (e.isStartDocument()) {
                StartDocument sd = (StartDocument) e;
                String strCharset = sd.getCharacterEncodingScheme();
                charset = Charset.forName(strCharset);
                XMLEvent event_out = eventFactory.createStartDocument(strCharset, "1.0");
                writer.add(event_out);
            } else {
                writer.add(e);
            }
        }

        if (data == null) {
            data = Collections.emptyMap();
        }
        InputStream renderInput = new ByteArrayInputStream(xmlOutput.toByteArray());
        ByteArrayOutputStream renderOutput = new ByteArrayOutputStream();
        this.renderer.render(renderInput, renderOutput, data, charset);

        InputStream input = new ByteArrayInputStream(renderOutput.toByteArray());
        postProcess(input,output);
    }

    private void postProcess(InputStream input, OutputStream output) throws Exception {
        TemplateXMLEventWriter eventWriter = new TemplateXMLEventWriter();
        // First, create a new XMLInputFactory
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        // Setup a new eventReader
        XMLEventReader reader = inputFactory.createXMLEventReader(input);
        this.postProcessor.process(reader, eventWriter);

        XMLOutputFactory outputXMLFactory = XMLOutputFactory.newInstance();
        outputXMLFactory.setProperty("escapeCharacters", false);

        XMLEventWriter writer = outputXMLFactory.createXMLEventWriter(output);

        Iterator<XMLEvent> it = eventWriter.interator();
        while (it.hasNext()) {
            XMLEvent e = it.next();
            if (e.isStartDocument()) {
                XMLEvent event_out = eventFactory.createStartDocument("UTF-8", "1.0");
                writer.add(event_out);
                writer.add(eventFactory.createCharacters(System.lineSeparator()));
            } else {
                writer.add(e);
            }
        }
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
