package com.proxeus.xml.template;

import com.proxeus.xml.processor.XMLEventProcessor;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class CleanTemplateExtractorMarkProcessor implements XMLEventProcessor {
    private XMLEventFactory eventFactory = XMLEventFactory.newInstance();

    @Override
    public void process(XMLEventReader reader, XMLEventWriter writer) throws XMLStreamException, IllegalStateException {
        nextEvent:
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            switch (event.getEventType()) {
                case XMLEvent.START_DOCUMENT:
                    writer.add(event);
                    writer.add(eventFactory.createCharacters(System.lineSeparator()));
                    break;
                case XMLEvent.START_ELEMENT:
                    StartElement s = event.asStartElement();
                    if (TemplateExtractor.IsMarked(s)){
                        s = TemplateExtractor.removeMark(s);
                    }
                    writer.add(s);
                    break;
                default:
                    writer.add(event);
            }
        }
    }


}
