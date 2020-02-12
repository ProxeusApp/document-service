package com.proxeus.xml.processor;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class XMLEventProcessorChain implements XMLEventProcessor {

    private List<XMLEventProcessor> processors;

    public XMLEventProcessorChain(XMLEventProcessor... processors) {
        this.processors = Arrays.asList(processors);
        if (processors.length == 0) {
            this.processors = Arrays.asList(new NoOpEventProcessor());
        }
    }

    public void addProcessor(XMLEventProcessor... processors) {
        for(XMLEventProcessor p:processors){
            this.processors.add(p);
        }
    }

    @Override
    public void process(XMLEventReader input, XMLEventWriter output) throws XMLStreamException, IllegalStateException {
        try {

            XMLEventReadWriter in = new XMLEventBuffer(input);
            XMLEventReadWriter out = null;

            Iterator<XMLEventProcessor> it = processors.iterator();
            while (it.hasNext()) {
                out = new XMLEventBuffer();
                it.next().process(in, out);
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
