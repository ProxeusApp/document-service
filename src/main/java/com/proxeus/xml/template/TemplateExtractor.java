package com.proxeus.xml.template;

import com.proxeus.xml.processor.XMLEventProcessor;
import com.proxeus.xml.template.parser.ParserState;
import com.proxeus.xml.template.parser.TagType;
import com.proxeus.xml.template.parser.TemplateParser;
import org.apache.log4j.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.*;
import java.util.LinkedList;
import java.util.List;
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
 * * XML tags closing inside code or comment island are pull forward before the code island,
 * * XML tags closing inside content island are pushed after the code island to preserve formatting and XML structure.
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
 * If an element spans several code islands, we need to split it if the element is closed in a different scope:
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
 * If an element spans several output or comment islands, we do not need to split it:
 *
 *                   {{..........}}          {%..........%}          {#.........#}
 *           <a>......................................................................</a>
 *
 * will results in:
 *
 *                   {{..........}}          {%..........%}          {#.........#}
 *           <a>.........................</a>              <a>........................</a>
 * }</pre>
 * <p>
 * Any element that is added or moved around is tagged with a "_proxeus" attribute.  This is useful for downstream
 * clean up processing.
 */
public class TemplateExtractor implements XMLEventProcessor {

    static public final QName PROXEUS_MARKER_ATTRIBUTE_NAME = new QName("_proxeus");
    static private final String PROXEUS_MARKER_ATTRIBUTE_VALUE = "template";

    private Logger log = Logger.getLogger(this.getClass());
    private TemplateParser parser;

    private LinkedList<ExtractorXMLEvent> tmpQueue;
    private LinkedList<ExtractorXMLEvent> elementStack;
    private LinkedList<ExtractorXMLEvent> resultQueue;


    static private XMLEventFactory eventFactory = XMLEventFactory.newInstance();

    public TemplateExtractor(TemplateParser parser) {
        this.parser = parser;
        this.parser.onProcessQueue(() -> processTmpQueue());
        this.parser.onFlushXmlCharacters(s -> flush(s));
        this.parser.onFlushTemplateCharacters(s -> flush(s));

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

        for (ExtractorXMLEvent event : resultQueue) {
            writer.add(event.getEvent());
        }
    }

    public static boolean IsMarked(StartElement s) {
        return s.getAttributeByName(TemplateExtractor.PROXEUS_MARKER_ATTRIBUTE_NAME) != null;
    }

    public static StartElement addMark(StartElement start) {
        List<Attribute> attributes = new LinkedList<>();
        attributes.add(eventFactory.createAttribute(PROXEUS_MARKER_ATTRIBUTE_NAME, PROXEUS_MARKER_ATTRIBUTE_VALUE));

        start.getAttributes().forEachRemaining(a -> {
            attributes.add((Attribute) a);
        });

        return eventFactory.createStartElement(start.getName(), attributes.iterator(), start.getNamespaces());
    }

    public static StartElement removeMark(StartElement start) {
        List<Attribute> attributes = new LinkedList<>();

        start.getAttributes().forEachRemaining(a -> {
            if (!((Attribute) a).getName().equals(PROXEUS_MARKER_ATTRIBUTE_NAME)) {
                attributes.add((Attribute) a);
            }
        });

        return eventFactory.createStartElement(start.getName(), attributes.iterator(), start.getNamespaces());
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
                        int level = 0; // This is to match the start element at the right level.
                        // If the start element is in the template, then we remove the start and the end elements.
                        if (tmpQueue.size() > 0) {
                            ListIterator<ExtractorXMLEvent> it = tmpQueue.listIterator(tmpQueue.size());
                            backtrackTmpQueue:
                            while (it.hasPrevious()) {
                                ExtractorXMLEvent e = it.previous();
                                switch (e.getEventType()) {
                                    case XMLEvent.END_ELEMENT:
                                        level++;
                                        continue;
                                    case XMLEvent.START_ELEMENT:
                                        StartElement s = e.asStartElement();
                                        if (level > 0) {
                                            level--;
                                            continue;
                                        }
                                        if (s.getName().equals(end.getName())) {
                                            log("Match start tag %s\n", e.toString());
                                            it.remove();
                                            ignore = true;
                                            break backtrackTmpQueue;
                                        }
                                        throw new IllegalStateException("XML not well formed, start and end tags do not match");
                                    default:
                                        continue;
                                }

                            }
                        }

                        if (!ignore) {
                            // We find the matching start element and clone it which will mark the element with special
                            // attribute.
                            if (resultQueue.size() > 0) {
                                ListIterator<ExtractorXMLEvent> it = resultQueue.listIterator(resultQueue.size());

                                backtrackResultQueue:
                                while (it.hasPrevious()) {
                                    ExtractorXMLEvent e = it.previous();
                                    switch (e.getEventType()) {
                                        case XMLEvent.END_ELEMENT:
                                            level++;
                                            continue;
                                        case XMLEvent.START_ELEMENT:
                                            StartElement s = e.asStartElement();
                                            if (level > 0) {
                                                level--;
                                                continue;
                                            }
                                            if (s.getName().equals(end.getName())) {
                                                log("Match start tag %s\n", e.toString());
                                                it.set(clone(e));
                                                break backtrackResultQueue;
                                            }
                                        default:
                                            continue;
                                    }

                                }
                            }
                            switch (parser.getTagType()) {
                                case CODE:
                                case COMMENT:
                                    // The end element is pushed before the code and comment islands
                                    pushResult(end, ParserState.XML, NONE);
                                    break;
                                case OUTPUT:
                                    // The end element is pushed after the output code island
                                    pushTmp(end, ParserState.XML, NONE);
                                    break;
                            }
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

                try {
                    log("PROCESS CHARACTERS -->%s<--\n", c.getData());
                    parser.process(c.getData());
                } catch (XMLStreamException e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
        }
    }

    private void log(String format, Object... args) {
        log.trace(String.format("<%s> <%s> <%d> %s", parser.getState(), parser.getTagType(), parser.getBlockId(), String.format(format, args)));
    }


    private ExtractorXMLEvent clone(ExtractorXMLEvent event) {
        return clone(event, event.getBlockId());
    }

    private ExtractorXMLEvent clone(ExtractorXMLEvent event, int blockId) {
        switch (event.getEventType()) {
            case XMLEvent.START_ELEMENT:
                StartElement start = event.asStartElement();
                return new ExtractorXMLEvent(addMark(start), event.getState(), event.getTagType(), blockId);
            case XMLEvent.END_ELEMENT:
                EndElement end = event.asEndElement();
                return new ExtractorXMLEvent(eventFactory.createEndElement(end.getName(), end.getNamespaces()), event.getState(), event.getTagType(), blockId);
            default:
                throw new IllegalStateException("not implemented");
        }
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

            // We have found a template code island.

            int startBlockId = previous.getBlockId();

            log("SPLIT CODE %s %s %s %s %s \n", eventType(previous), previous.toString(), previous.getState(), previous.getTagType(), previous.getBlockId());

            // Wrap the code island with </...> <...>
            it.next();

            it.add(clone(start, startBlockId));
            it.previous();
            it.previous();

            int endBlockId = it.previous().getBlockId();
            it.next();
            it.add(clone(end, endBlockId));
            if (endBlockId == start.getBlockId()) {
                // We closed the element in the right block.
                // We find the start element and clone it in order to mark it.
                while (it.hasPrevious()) {
                    previous = it.previous();
                    if (previous == start) {
                        it.set(clone(previous));
                        break;
                    }
                }
                break;
            }
        }
    }


    private EndElement clone(EndElement s) {
        return eventFactory.createEndElement(s.getName(), s.getNamespaces());
    }

    private void pushTmp(XMLEvent event) {
        pushTmp(event, parser.getState(), parser.getTagType(), parser.getBlockId());
    }

    private void pushTmp(XMLEvent event, ParserState state, TagType tagType) {
        pushTmp(event, state, tagType, parser.getBlockId());
    }

    private void pushTmp(XMLEvent event, ParserState state, TagType tagType, int blockId) {
        log("PUSH EVENT TO TMP QUEUE %d %s\n", event.getEventType(), event.toString());

        ExtractorXMLEvent e = new ExtractorXMLEvent(event, state, tagType, blockId);
        if (e.getEventType() == XMLEvent.START_ELEMENT) {
            e = clone(e);
        }
        tmpQueue.add(e);
    }

    private void flush(String characters) {
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
