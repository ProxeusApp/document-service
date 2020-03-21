package com.proxeus.xml.template;

import com.proxeus.xml.processor.XMLEventProcessor;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.LinkedList;
import java.util.List;

public class CleanEmptyElementProcessor implements XMLEventProcessor {
    private List<QName> emptyElementToRemove;
    LinkedList<XMLEvent> queue = new LinkedList<>();

    private XMLEventFactory eventFactory = XMLEventFactory.newInstance();

    public CleanEmptyElementProcessor(List<QName> emptyElementToRemove) {
        this.emptyElementToRemove = emptyElementToRemove;
    }

    @Override
    public void process(XMLEventReader reader, XMLEventWriter writer) throws XMLStreamException, IllegalStateException {
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            switch (event.getEventType()) {
                case XMLEvent.START_DOCUMENT:
                    writer.add(event);
                    writer.add(eventFactory.createCharacters(System.lineSeparator()));
                    break;
                case XMLEvent.START_ELEMENT:
                    StartElement s = event.asStartElement();
                    queue.offer(s);
                    continue;
                case XMLEvent.CHARACTERS:
                    Characters c = event.asCharacters();
                    if (!c.isIgnorableWhiteSpace()) {
                        flush(c, writer);
                    }
                    continue;
                case XMLEvent.END_ELEMENT:
                    EndElement e = event.asEndElement();
                    if (queue.isEmpty()) {
                        flush(e, writer);
                        continue;
                    }

                    if (!emptyElementToRemove.contains(e.getName())) {
                        flush(e, writer);
                        continue;
                    }

                    XMLEvent previous = queue.peekLast();
                    if (!previous.isStartElement() || !previous.asStartElement().getName().equals(e.getName())) {
                        flush(e, writer);
                        continue;
                    }

                    queue.removeLast();

                    continue;
                default:
                    flush(event, writer);
            }
        }
    }

    private void flush(XMLEvent event, XMLEventWriter writer) {
        queue.offer(event);
        while (!queue.isEmpty()) {
            try {
                writer.add(queue.pollFirst());
            } catch (Exception e) {
                // pollFirst only throws exception if the queue is empty which cannot happen here.
            }
        }
    }
}
