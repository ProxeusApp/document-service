package com.proxeus.xml.jtwig;

import java.io.OutputStream;

import javax.xml.stream.*;
import javax.xml.stream.events.*;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.LinkedList;
import java.util.ListIterator;

import static com.proxeus.xml.jtwig.ExtractorState.*;
import static com.proxeus.xml.jtwig.IslandType.*;

/**
 * This class extract JTwig template code island (http://jtwig.org/documentation/reference/syntax/code-islands)
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
public class XMLJTwigExtractor {

    private ExtractorState state;
    private IslandType islandType;

    private StringBuffer nextCharacters;
    private StringBuffer whiteSpaces;
    private StringBuffer nextIsland;
    private LinkedList<ExtractorXMLEvent> afterIsland;
    private LinkedList<ExtractorXMLEvent> beforeIsland;
    private LinkedList<ExtractorXMLEvent> waitQueue;
    private LinkedList<ExtractorXMLEvent> resultQueue;

    private XMLEventFactory eventFactory;

    // DEBUG
    private StringBuffer allString;

    public XMLJTwigExtractor() {
        this.state = XML;
        this.nextCharacters = new StringBuffer();
        this.whiteSpaces = new StringBuffer();
        this.nextIsland = new StringBuffer();

        this.afterIsland = new LinkedList<>();
        this.beforeIsland = new LinkedList<>();
        this.waitQueue = new LinkedList<>();
        this.resultQueue = new LinkedList<>();
        this.eventFactory = XMLEventFactory.newInstance();

        // DEBUG
        this.allString = new StringBuffer();
    }

    @SuppressWarnings({"unchecked", "null"})
    public void extract(XMLEventReader eventReader, OutputStream output) throws XMLStreamException, IllegalStateException {
        while (eventReader.hasNext()) {
            processEvent(eventReader.nextEvent());
        }

        System.out.println(allString.toString());

        System.out.println();

        XMLEventWriter ew = XMLOutputFactory.newInstance().createXMLEventWriter(output);
        resultQueue.forEach(event -> {
            try {
                ew.add(event.getEvent());
            } catch (XMLStreamException e) {

            }
        });
    }

    private void processEvent(XMLEvent event) throws XMLStreamException {
        System.out.printf("<%s> <%s> <%s> ", state, islandType, eventType(event));

        switch (event.getEventType()) {
            case XMLEvent.START_DOCUMENT:
                System.out.println();
                resultQueue.add(new ExtractorXMLEvent(event, state, islandType));
                resultQueue.add(new ExtractorXMLEvent(eventFactory.createCharacters("\n"), state, islandType));
                break;

            case XMLEvent.END_DOCUMENT:
                System.out.println();
                if (this.state != XML) {
                    throw new IllegalStateException("Template code island not terminated");
                }
                resultQueue.add(new ExtractorXMLEvent(event, state, islandType));
                break;
            case XMLEvent.START_ELEMENT:
                StartElement start = event.asStartElement();
                System.out.println("\t" + start.getName());
                switch (state) {
                    case XML:
                        resultQueue.add(new ExtractorXMLEvent(event, state, islandType));
                        break;
                    case MAYBE_BEGIN_ISLAND:
                        System.out.printf("PUSH ************************* START_ELEMENT TO WAIT QUEUE %s\n", start.getName());
                        waitQueue.add(new ExtractorXMLEvent(event, state, islandType));
                        break;
                    case ISLAND:
                        System.out.printf("PUSH ************************* START_ELEMENT TO AFTER QUEUE %s\n", start.getName());
                        afterIsland.add(new ExtractorXMLEvent(event, state, islandType));
                }
                break;
            case XMLEvent.END_ELEMENT:
                EndElement end = event.asEndElement();
                System.out.println("\t" + end.getName());
                switch (state) {
                    case XML:
                        resultQueue.add(new ExtractorXMLEvent(event, state, islandType));
                        break;
                    case MAYBE_BEGIN_ISLAND:
                        System.out.printf("PUSH ************************* END_ELEMENT TO WAIT QUEUE %s\n", end.getName());
                        waitQueue.add(new ExtractorXMLEvent(event, state, islandType));
                        break;
                    case ISLAND:
                        System.out.printf("BEGIN ************************* END_ELEMENT IN ISLAND %s\n", end.getName());
                        ListIterator<ExtractorXMLEvent> it = afterIsland.listIterator(0);
                        boolean ignore = false;
                        while (it.hasNext()) {
                            ExtractorXMLEvent e = it.next();
                            System.out.printf("In stack %s %d\n", e.toString(), e.getEventType());
                            if (!e.isStartElement()) {
                                continue;
                            }
                            StartElement s = e.asStartElement();
                            System.out.printf("start tag >%s< >%s<\n", s.getName(), end.getName());
                            if (s.getName().equals(end.getName())) {
                                System.out.printf("Match start tag %s\n", e.toString());
                                it.remove();
                                ignore = true;
                                break;
                            }
                            System.out.printf("Wrong start tag %s != %s\n", s.getName(), end.getName());
                            // A well formed XML file should not reach this line as the end element shoudl match
                            // the start element.
                        }
                        System.out.printf("END ************************* END_ELEMENT IN ISLAND %s\n", end.getName());
                        if (!ignore) {
                            System.out.printf("PUSH ************************* END_ELEMENT TO RESULT QUEUE %s\n", end.getName());
                            resultQueue.add(new ExtractorXMLEvent(event, state, islandType));
                        }
                }

                break;
            case XMLEvent.CHARACTERS:
                Characters c = event.asCharacters();
                if (c.isIgnorableWhiteSpace()) {
                    resultQueue.add(new ExtractorXMLEvent(event, state, islandType));
                    break;
                }
                if (c.isCData()) {
                    resultQueue.add(new ExtractorXMLEvent(event, state, islandType));
                    break;
                }
                System.out.println();
                System.out.println("-->" + c.getData() + "<--");

                CharacterIterator it = new StringCharacterIterator(c.getData());
                while (it.current() != CharacterIterator.DONE) {
                    switch (this.state) {
                        case XML:
                            switch (it.current()) {
                                case '{':
                                    this.state = MAYBE_BEGIN_ISLAND;
                                    nextIsland.append(it.current());
                                    break;
                                default:
                                    nextCharacters.append(it.current());
                            }
                            break;
                        case MAYBE_BEGIN_ISLAND:
                            /*if (Character.isWhitespace(it.current())) {
                                whiteSpaces.append(it.current());
                                break;
                            }
                             */

                            switch (it.current()) {
                                case '%':
                                    this.state = ISLAND;
                                    this.islandType = CODE;
                                    processWaitQueue();
                                    nextIsland.append(it.current());
                                    //         whiteSpaces.delete(0, whiteSpaces.length());
                                    break;
                                case '{':
                                    this.state = ISLAND;
                                    this.islandType = OUTPUT;
                                    processWaitQueue();
                                    nextIsland.append(it.current());
                                    //        whiteSpaces.delete(0, whiteSpaces.length());
                                    break;
                                case '#':
                                    this.state = ISLAND;
                                    this.islandType = COMMENT;
                                    processWaitQueue();
                                    nextIsland.append(it.current());
                                    //       whiteSpaces.delete(0, whiteSpaces.length());
                                    break;
                                default:
                                    this.state = XML;
                                    // push nextIsland to result and reset island
                                    // push any tags and reset stack
                                    // deal with spaces
                                    // current to next characters
                                    nextCharacters.append(nextIsland);
                                    nextIsland.delete(0, nextIsland.length());

                                    if (afterIsland.size() > 0) {
                                        resultQueue.add(new ExtractorXMLEvent(eventFactory.createCharacters(nextCharacters.toString()), state, islandType));
                                        nextCharacters.delete(0, nextCharacters.length());
                                        System.out.printf("PUSH ************************* BEFORE QUEUE TO RESULT QUEUE %s\n", afterIsland.toString());
                                        resultQueue.addAll(waitQueue);
                                        System.out.printf("RESULT QUEUE %s\n", resultQueue.toString());
                                        waitQueue.clear();
                                    }
                                    //      nextCharacters.append(whiteSpaces);
                                    nextCharacters.append(it.current());

                                    // whiteSpaces.delete(0, whiteSpaces.length());
                            }
                            break;
                        case ISLAND:
                            if (Character.isWhitespace(it.current())) {
                                if (!Character.isWhitespace(nextIsland.charAt(nextIsland.length() - 1))) {
                                    nextIsland.append(' ');
                                }
                            } else {
                                nextIsland.append(it.current());
                            }
                            switch (it.current()) {
                                case '%':
                                    if (this.islandType == CODE) {
                                        this.state = MAYBE_END_ISLAND;
                                    }
                                    break;
                                case '}':
                                    if (this.islandType == OUTPUT) {
                                        this.state = MAYBE_END_ISLAND;
                                    }
                                    break;
                                case '#':
                                    if (this.islandType == COMMENT) {
                                        this.state = MAYBE_END_ISLAND;
                                    }
                                    break;
                                case '"':
                                    this.state = DOUBLE_QUOTE_STRING;
                                    break;
                                case '\'':
                                    this.state = SINGLE_QUOTE_STRING;
                            }
                            break;
                        case DOUBLE_QUOTE_STRING:
                            nextIsland.append(it.current());
                            switch (it.current()) {
                                case '"':
                                    this.state = ISLAND;
                            }
                            break;
                        case SINGLE_QUOTE_STRING:
                            nextIsland.append(it.current());
                            switch (it.current()) {
                                case '\'':
                                    this.state = ISLAND;
                            }
                            break;
                        case MAYBE_END_ISLAND:
                           /* if (Character.isWhitespace(it.current())) {
                                whiteSpaces.append(it.current());
                                break;
                            }
                            */

                            switch (it.current()) {
                                case '}':
                                    this.state = XML;
                                    //whiteSpaces.delete(0, whiteSpaces.length());
                                    nextIsland.append(it.current());
                                    nextCharacters.append(nextIsland);
                                    nextIsland.delete(0, nextIsland.length());

                                    if (afterIsland.size() > 0) {
                                        resultQueue.add(new ExtractorXMLEvent(eventFactory.createCharacters(nextCharacters.toString()), state, islandType));
                                        nextCharacters.delete(0, nextCharacters.length());
                                        System.out.printf("PUSH ************************* AFTER QUEUE TO RESULT QUEUE %s\n", afterIsland.toString());
                                        resultQueue.addAll(afterIsland);
                                        System.out.printf("RESULT QUEUE %s\n", resultQueue.toString());
                                        afterIsland.clear();
                                    }

                                    break;
                                default:
                                    this.state = ISLAND;
                                    //nextIsland.append(whiteSpaces);
                                    //whiteSpaces.delete(0, whiteSpaces.length());
                                    nextIsland.append(it.current());
                            }
                    }
                    it.next();
                }
                if (nextCharacters.length() > 0) {
                    resultQueue.add(new ExtractorXMLEvent(eventFactory.createCharacters(nextCharacters.toString()), state, islandType));
                    nextCharacters.delete(0, nextCharacters.length());
                }
            default:
        }
    }

    private void processWaitQueue() throws XMLStreamException {
        System.out.printf("BEGIN ************************* ************************* PROCESSING WAIT QUEUE %s before %s after: %s\n", waitQueue.toString(), beforeIsland.toString(), afterIsland.toString());
        for (ExtractorXMLEvent e : waitQueue) {
            processEvent(e.getEvent());
        }
        System.out.printf("END ************************* ************************* PROCESSING WAIT QUEUE %s\n", resultQueue.toString());
        waitQueue.clear();
    }

    private String eventType(XMLEvent event) {
        switch (event.getEventType()) {
            case XMLEvent.START_DOCUMENT:
                return "START DOCUMENT";
            case XMLEvent.END_DOCUMENT:
                return "END DOCUMENT";
            case XMLEvent.START_ELEMENT:
                return "START ELEMENT";
            case XMLEvent.END_ELEMENT:
                return "END ELEMENT";
            case XMLEvent.CHARACTERS:
                return "CHARACTERS";
            default:
                return "UNKNOWN";
        }
    }
}
