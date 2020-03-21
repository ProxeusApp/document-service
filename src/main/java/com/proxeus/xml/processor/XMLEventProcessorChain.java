package com.proxeus.xml.processor;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class XMLEventProcessorChain implements XMLEventProcessor {

    private List<XMLEventProcessor> processors = new LinkedList<>();

    public XMLEventProcessorChain(XMLEventProcessor... processors) {
        this.processors.addAll(Arrays.asList(processors));
        if (processors.length == 0) {
            this.processors.add(new NoOpEventProcessor());
        }
    }

    public void addProcessor(XMLEventProcessor... processors) {
        this.processors.addAll(Arrays.asList(processors));
    }

    @Override
    public void process(XMLEventReader input, XMLEventWriter output) throws XMLStreamException, IllegalStateException {
        try {

            XMLEventReadWriter in = new XMLEventBuffer(input);
            XMLEventReadWriter out = null;

            for (XMLEventProcessor processor : processors) {
                out = new XMLEventBuffer();
                processor.process(in, out);
                in = out;
            }

            while (out.hasNext()) {
                output.add(out.nextEvent());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
