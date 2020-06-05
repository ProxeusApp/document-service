package com.proxeus.xml.template;

import com.proxeus.xml.processor.XMLEventProcessor;
import com.proxeus.xml.template.parser.ParserState;
import com.proxeus.xml.template.parser.TagType;
import com.proxeus.xml.template.parser.TemplateParser;
import org.apache.log4j.Logger;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.LinkedList;
import java.util.ListIterator;

import static com.proxeus.xml.template.parser.TagType.CODE;
import static com.proxeus.xml.template.parser.TagType.NONE;

/**
 * This class process JTwig template code island (http://jtwig.org/documentation/reference/syntax/code-islands)
 * located in the XML text nodes and transform the XML document to a JTwig template.
 * <p>
 * <p>
 * First, we need to ensure that code islands do not contain any XML tags:
 * * XML elements entirely inside a code island are deleted,
 * * XML tags opening inside an island are push after the end of the code island,
 * * XML tags closing inside code island are pull forward before the code island.
 *
 * <pre>{@Code
 *                        {%...........................%}            {% ......%}
 *              <a>...</a>
 *              <b>..........</b>                 <c>.......</c>
 *                                    <d>...</d>  <e>..................</e>
 *
 * will result in :
 *
 *                        {% ..........................%}            {% ......%}
 *             <a>...</a>
 *             <b>...</b>                               <c>....</c>
 *                                                      <e>....</e>
 * }</pre>
 *
 *
 * <pre>{@Code
 * If an element span several code islands, we need to split them:
 *
 *                   {%..........%}          {%..........%}          {%.........%}
 *           <a>......................................................................</a>
 *
 *
 * will results in:
 *
 *                   {%..........%}          {%..........%}          {%.........%}
 *         <a>...</a>              <a>...</a>              <a>...</a>             <a>...</a>
 * }</pre>
 *
 * <pre>{@Code
 * If an element span an output or a comment island, we do not need to split it:
 *
 *                   {{..........}}          {%..........%}          {#.........#}
 *           <a>......................................................................</a>
 *
 * will results in:
 *
 *                   {{..........}}          {%..........%}          {#.........#}
 *           <a>.........................</a>              <a>........................</a>
 * }</pre>
 */
public class TemplateExtractor implements XMLEventProcessor {

    private Logger log = Logger.getLogger(this.getClass());
    private TemplateParser parser;

    private LinkedList<ExtractorXMLEvent> tmpQueue;
    private LinkedList<ExtractorXMLEvent> elementStack;
    private LinkedList<ExtractorXMLEvent> resultQueue;

    private XMLEventFactory eventFactory = XMLEventFactory.newInstance();

    public TemplateExtractor(TemplateParser parser) {
        this.parser = parser;
        this.tmpQueue = new LinkedList<>();
        this.elementStack = new LinkedList<>();
        this.resultQueue = new LinkedList<>();
    }

    @Override
    @SuppressWarnings({"unchecked", "null"})
    public void process(XMLEventReader reader, XMLEventWriter writer) throws XMLStreamException, IllegalStateException {
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            processEvent(event);
        }

        for(ExtractorXMLEvent event : resultQueue){
            writer.add(event.getEvent());
        }
    }


    private void processEvent(XMLEvent event) {
        log("PROCESS EVENT<%s>\n", eventType(event));
        switch (event.getEventType()) {
            case XMLEvent.START_DOCUMENT:
                pushResult(event);
                pushResult(eventFactory.createCharacters(System.lineSeparator()));
                break;
            case XMLEvent.END_DOCUMENT:
                if (this.parser.getState() != ParserState.XML) {
                    throw new IllegalStateException("Template code island not terminated");
                }
                pushResult(event);
                break;
            case XMLEvent.START_ELEMENT:
                switch (parser.getState()) {
                    case XML:
                        pushResult(event);

                        break;
                    case MAYBE_START_DELIMITER:
                    case MAYBE_END_DELIMITER:
                    case TEMPLATE:
                        pushTmp(event);
                }
                break;
            case XMLEvent.END_ELEMENT:
                EndElement end = event.asEndElement();
                switch (parser.getState()) {
                    case XML:
                        pushResult(event);
                        break;
                    case MAYBE_START_DELIMITER:
                    case MAYBE_END_DELIMITER:
                        pushTmp(event);
                        break;
                    case TEMPLATE:
                        log("BEGIN END_ELEMENT IN TEMPLATE %s tmp: %s result: %s\n", end.getName(), tmpQueue.toString(), resultQueue.toString());
                        boolean ignore = false;
                        // If the start element is in the template, then we remove the start and the end elements.
                        if (tmpQueue.size() > 0) {
                            ListIterator<ExtractorXMLEvent> it = tmpQueue.listIterator(tmpQueue.size());
                            while (it.hasPrevious()) {
                                ExtractorXMLEvent e = it.previous();
                                if (!e.isStartElement()) {
                                    continue;
                                }
                                StartElement s = e.asStartElement();
                                if (s.getName().equals(end.getName())) {
                                    log("Match start tag %s\n", e.toString());
                                    it.remove();
                                    ignore = true;
                                    break;
                                }
                                throw new IllegalStateException("XML not well formed, start and end tags do not match");
                            }
                        }

                        if (!ignore) {
                            // The end element is pushed before the code island
                            pushResult(event, ParserState.XML, NONE);
                        }
                        log("END END_ELEMENT IN TEMPLATE %s\n", end.getName());
                }

                break;
            case XMLEvent.CHARACTERS:
                Characters c = event.asCharacters();
                if (c.isIgnorableWhiteSpace()) {
                    break;
                }
                if (c.isCData()) {
                    pushResult(event);
                    break;
                }

                parser.onProcessQueue(s -> processTmpQueue());
                parser.onFlushCharacters(e -> flush(e.getCharacters(), e.getState(), e.getTagType()));
                try {
                    log("PROCESS CHARACTERS -->%s<--\n", c.getData());
                    parser.process(c.getData());
                } catch (XMLStreamException e) {
                    throw new RuntimeException(e);
                }

            default:
        }
    }

    private void log(String format, Object... args) {
        log.trace(String.format("<%s> <%s> <%d> %s", parser.getState(), parser.getTagType(), parser.getBlockId(), String.format(format, args)));
    }

    private void pushResult(XMLEvent event) {
        pushResult(event, parser.getState(), parser.getTagType(), parser.getBlockId());
    }

    private void pushResult(XMLEvent event, ParserState state, TagType tagType) {
        pushResult(event, state, tagType, parser.getBlockId());
    }

    private void pushResult(XMLEvent event, ParserState state, TagType tagType, int blockId) {
        log("PUSH EVENT TO RESULT QUEUE %s -->%s<--\n", eventType(event), event.toString());
        ExtractorXMLEvent e = new ExtractorXMLEvent(event, state, tagType, blockId);
        switch (e.getEvent().getEventType()) {
            case XMLEvent.START_ELEMENT:
                resultQueue.add(e);
                elementStack.push(e);
                break;
            case XMLEvent.END_ELEMENT:
                resultQueue.add(e);
                ExtractorXMLEvent stacktHead = elementStack.pop();
                splitSpanningElement(stacktHead, e);
                break;
            default:
                resultQueue.add(e);
        }
    }

    private void splitSpanningElement(ExtractorXMLEvent start, ExtractorXMLEvent end) {
        log("SPLIT SPANNING ELEMENT %s %s\n", eventType(start), start.toString());
        if (start.getBlockId() == end.getBlockId()) {
            return;
        }

        int endIndex = resultQueue.indexOf(end);
        if (endIndex == -1) {
            return;
        }

        ListIterator<ExtractorXMLEvent> it = resultQueue.listIterator(endIndex);
        while (it.hasPrevious()) {
            ExtractorXMLEvent previous = it.previous();
            if (previous == start) {
                break;
            }

            // The assumption is that a code island is contained in its own characters event.
            // This has to be enforced by the template parser.
            if (previous.getTagType() != CODE) {
                continue;
            }

            log("SPLIT CODE %s %s %s %s %s \n", eventType(previous), previous.toString(), previous.getState(), previous.getTagType(), previous.getBlockId());
            int blockId = previous.getBlockId();
            // Wrap the code island with </...> <...>

            it.next();

            it.add(new ExtractorXMLEvent(clone(start.getEvent()), ParserState.XML, NONE, blockId));
            it.previous();
            it.previous();

            blockId = it.previous().getBlockId();
            it.next();
            it.add(new ExtractorXMLEvent(clone(end.getEvent()), ParserState.XML, NONE, blockId));
            if (blockId == start.getBlockId()) {
                // We closed the element in the right block.
                break;
            }
        }
    }

    private XMLEvent clone(XMLEvent event) {
        switch (event.getEventType()) {
            case XMLEvent.START_ELEMENT:
                StartElement start = event.asStartElement();
                return eventFactory.createStartElement(start.getName(), start.getAttributes(), start.getNamespaces());
            case XMLEvent.END_ELEMENT:
                EndElement end = event.asEndElement();
                return eventFactory.createEndElement(end.getName(), end.getNamespaces());
            default:
                throw new IllegalStateException("not implemented");
        }
    }

    private EndElement clone(EndElement s) {
        return eventFactory.createEndElement(s.getName(), s.getNamespaces());
    }

    private void pushStack(XMLEvent event) {
        log("PUSH EVENT TO STACK %d %s\n", event.getEventType(), event.toString());
        elementStack.push(new ExtractorXMLEvent(event, parser.getState(), parser.getTagType(), parser.getBlockId()));
    }

    private void pushTmp(XMLEvent event) {
        log("PUSH EVENT TO TMP QUEUE %d %s\n", event.getEventType(), event.toString());
        tmpQueue.add(new ExtractorXMLEvent(event, parser.getState(), parser.getTagType(), parser.getBlockId()));
    }

    private void flush(String characters, ParserState state, TagType tagType) {
        pushResult(eventFactory.createCharacters(characters));
    }

    private void processTmpQueue() {
        log("BEGIN PROCESSING TMP QUEUE %s tmp: %s\n", resultQueue.toString(), tmpQueue.toString());
        if (tmpQueue.size() > 0) {
            // Cloning the tmpQueue as we are re-entering the processor.
            LinkedList<ExtractorXMLEvent> clone = new LinkedList<>();
            clone.addAll(tmpQueue); //
            tmpQueue.clear();
            for (ExtractorXMLEvent e : clone) {
                processEvent(e.getEvent());
            }
        }
        log("END PROCESSING TMP QUEUE %s\n", resultQueue.toString());
    }

    private String eventType(XMLEvent event) {
        switch (event.getEventType()) {
            case XMLEvent.START_DOCUMENT:
                return "START DOCUMENT";
            case XMLEvent.END_DOCUMENT:
                return "END DOCUMENT";
            case XMLEvent.START_ELEMENT:
                return "START ELEMENT " + event.asStartElement().getName();
            case XMLEvent.END_ELEMENT:
                return "END ELEMENT " + event.asEndElement().getName();
            case XMLEvent.CHARACTERS:
                return "CHARACTERS";
            default:
                return "UNKNOWN";
        }
    }
}
