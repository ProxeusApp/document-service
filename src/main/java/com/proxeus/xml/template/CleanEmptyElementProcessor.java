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
import java.util.ListIterator;

public class CleanEmptyElementProcessor implements XMLEventProcessor {
    private List<QName> elementToRemoveIfEmpty;
    private List<QName> elementToRemoveIfOnlyWhitespace;

    LinkedList<XMLEvent> queue = new LinkedList<>();

    private XMLEventFactory eventFactory = XMLEventFactory.newInstance();

    private boolean inTemplate;

    public CleanEmptyElementProcessor(List<QName> ElementToRemoveIfEmpty, List<QName> elementToRemoveIfOnlyWhitespace) {
        this.elementToRemoveIfEmpty = ElementToRemoveIfEmpty;
        this.elementToRemoveIfOnlyWhitespace = elementToRemoveIfOnlyWhitespace;
        this.inTemplate = false;
    }

    @Override
    public void process(XMLEventReader reader, XMLEventWriter writer) throws XMLStreamException, IllegalStateException {
        nextEvent:
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            switch (event.getEventType()) {
                case XMLEvent.START_DOCUMENT:
                    // writer.add(event);
                    queue.offer(eventFactory.createStartDocument("UTF-8", "1.0"));
                    queue.offer(eventFactory.createCharacters(System.lineSeparator()));
                    break;
                case XMLEvent.START_ELEMENT:
                    StartElement s = event.asStartElement();
                    this.inTemplate = TemplateExtractor.IsMarked(s);
                    queue.offer(s);
                    break;
                case XMLEvent.CHARACTERS:
                    Characters c = event.asCharacters();
                    if (c.isIgnorableWhiteSpace()) {
                        break;
                    }
                    queue.offer(c);
                    if (c.isWhiteSpace()) {
                        break;
                    }
                    flush(writer);
                    break;
                case XMLEvent.END_ELEMENT:
                    EndElement e = event.asEndElement();
                    if (queue.isEmpty()) {
                        queue.offer(e);
                        flush(writer);
                        break;
                    }

                    if (!(elementToRemoveIfEmpty.contains(e.getName()) || elementToRemoveIfOnlyWhitespace.contains(e.getName()))) {
                        queue.offer(e);
                        flush(writer);
                        break;
                    }

                    XMLEvent previous = queue.peekLast();
                    if (inTemplate && previous.isStartElement() && previous.asStartElement().getName().equals(e.getName())) {
                        queue.removeLast();
                        break;
                    }

                    if (elementToRemoveIfOnlyWhitespace.contains(e.getName())) {
                        // Here we backtrack the queue for the next start element that can be removed when only containing whitespaces.
                        ListIterator<XMLEvent> it = queue.listIterator(queue.size() - 1);
                        backtrack:
                        while (it.hasPrevious()) {
                            XMLEvent p = it.previous();
                            switch (p.getEventType()) {
                                case XMLEvent.CHARACTERS:
                                    // The queue can only contain whitespaces at this point
                                    continue backtrack;
                                case XMLEvent.START_ELEMENT:
                                    if (inTemplate && p.asStartElement().getName().equals(e.getName())) {
                                        it.remove();
                                        while (it.hasNext()) {
                                            it.next();
                                            it.remove();
                                        }
                                        continue nextEvent;
                                    }
                                    break backtrack;
                                default:
                                    break backtrack;
                            }
                        }
                    }

                    queue.offer(e);
                    flush(writer);
                    break;
                default:
                    queue.offer(event);
                    flush(writer);
            }
        }
    }


    private void flush(XMLEventWriter writer) {
        this.inTemplate = false;
        while (!queue.isEmpty()) {
            try {
                writer.add(queue.pollFirst());
            } catch (Exception e) {
                // pollFirst only throws exception if the queue is empty which cannot happen here.
            }
        }
    }
}
