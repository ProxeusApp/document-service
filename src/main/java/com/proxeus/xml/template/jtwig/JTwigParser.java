package com.proxeus.xml.template.jtwig;

import com.proxeus.xml.template.parser.*;

import javax.xml.stream.XMLStreamException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.proxeus.xml.template.parser.ParserState.*;
import static com.proxeus.xml.template.parser.TagType.*;

/**
 *
 */
public class JTwigParser implements TemplateParser, TemplateParserFactory {

    private ParserState state;
    private TagType tagType;
    private boolean ignoreSpace;
    private char stringDelimiter;

    private StringBuffer nextCharacters;

    private int blockCounter;
    private LinkedList<Integer> blockStack;

    private Consumer<String> onFlushXmlCharacters;
    private Consumer<String> onFlushTemplateCharacters;
    private Runnable onProcessQueue;

    private static Set<String> blockStart = new HashSet<>(Arrays.asList("if", "elseif", "else", "for", "block", "embed", "macro", "autoescape", "filter", "verbatim"));
    private static Set<String> blockEnd = new HashSet<>(Arrays.asList("elseif", "else", "endif", "endfor", "endblock", "endembed", "endmacro", "endautoescape", "endfilter", "endverbatim"));

    // Block processors are JTWig tags that process content between the start and end tags.  The full construct should be handled as one template element and any added XML tags should be
    // push around it
    private static Set<String> blockProcessor = new HashSet<>(Arrays.asList("autoescape", "filter", "verbatim"));


    public JTwigParser() {
        this.state = XML;
        this.tagType = NONE;
        this.nextCharacters = new StringBuffer();
        this.blockStack = new LinkedList<>();
    }

    @Override
    public TemplateParser newInstance() {
        return new JTwigParser();
    }

    @Override
    public ParserState getState() {
        return this.state;
    }

    @Override
    public TagType getTagType() {
        return this.tagType;
    }

    @Override
    public int getBlockId() {
        if (blockStack.size() == 0) {
            return 0;
        }
        return blockStack.peek();
    }

    private int pushBlock() {
        blockCounter++;
        blockStack.push(blockCounter);
        return blockCounter;
    }

    private int popBlock() {
        if (blockStack.size() > 0) {
            blockStack.pop();
        }
        return getBlockId();
    }

    @Override
    public void onFlushXmlCharacters(Consumer<String> onFlushXmlCharacters) {
        this.onFlushXmlCharacters = onFlushXmlCharacters;
    }

    @Override
    public void onFlushTemplateCharacters(Consumer<String> onFlushTemplateCharacters) {
        this.onFlushTemplateCharacters = onFlushTemplateCharacters;
    }

    @Override
    public void onProcessQueue(Runnable onProcessQueue) {
        this.onProcessQueue = onProcessQueue;
    }

    @Override
    public void process(String characters) throws XMLStreamException {
        CharacterIterator it = new StringCharacterIterator(characters);
        while (it.current() != CharacterIterator.DONE) {
            process(it.current());
            it.next();
        }
        if (state == ParserState.XML && nextCharacters.length() > 0) {
            flushXmlCharacters(nextCharacters);
        }
    }

    private void process(char c) throws XMLStreamException {
        switch (this.state) {
            case XML:
                switch (c) {
                    case '{':
                        // We flush the current XML characters to ensure that template
                        // characters are contained in their own character events.
                        flushXmlCharacters(nextCharacters);
                        stateChange(MAYBE_START_DELIMITER, NONE);
                        nextCharacters.append(c);
                        break;
                    default:
                        nextCharacters.append(c);
                }
                break;
            case MAYBE_START_DELIMITER:
                if (Character.isWhitespace(c)) {
                    break;
                }
                switch (c) {
                    case '%':
                        stateChange(TEMPLATE, CODE);
                        processQueue();
                        nextCharacters.append(c);
                        break;
                    case '{':
                        stateChange(TEMPLATE, OUTPUT);
                        processQueue();
                        nextCharacters.append(c);
                        break;
                    case '#':
                        stateChange(TEMPLATE, COMMENT);
                        processQueue();
                        nextCharacters.append(c);
                        break;
                    default:
                        stateChange(XML, NONE);
                        flushXmlCharacters(nextCharacters);
                        processQueue();
                        nextCharacters.append(c);
                }
                break;
            case TEMPLATE:
                if (Character.isWhitespace(c) && c != ' ') {
                    ignoreSpace = true;
                    break;
                }
                if (c == ' ' && ignoreSpace) {
                    break;
                }
                ignoreSpace = false;

                if (c == ' ' && nextCharacters.length() > 0 && nextCharacters.charAt(nextCharacters.length() - 1) == ' ') {
                    break;
                }

                c = cleanQuote(c);
                nextCharacters.append(c);

                switch (c) {
                    case '%':
                        if (this.tagType == CODE) {
                            stateChange(MAYBE_END_DELIMITER);
                        }
                        break;
                    case '}':
                        if (this.tagType == OUTPUT) {
                            stateChange(MAYBE_END_DELIMITER);
                        }
                        break;
                    case '#':
                        if (this.tagType == COMMENT) {
                            stateChange(MAYBE_END_DELIMITER);
                        }
                        break;
                    case '"':
                    case '\'':
                        stateChange(STRING);
                        stringDelimiter = c;
                        ignoreSpace = false;
                }
                break;
            case STRING:
                if (Character.isWhitespace(c) && c != ' ') {
                    ignoreSpace = true;
                    break;
                }
                if (c == ' ' && ignoreSpace) {
                    break;
                }
                ignoreSpace = false;

                c = cleanQuote(c);
                nextCharacters.append(c);

                if (c == stringDelimiter) {
                    stateChange(TEMPLATE);
                }
                break;
            case MAYBE_END_DELIMITER:
                stateChange(TEMPLATE);
                nextCharacters.append(c);

                switch (c) {
                    case '}':
                        processQueue();

                        if (tagType == CODE) {
                            // This ensure that the code island block id is the same as the block id of the following block.
                            //
                            //      {%if ... %} ...  {% endif %}
                            //    0      1       1        0       0
                            updateBlock(nextCharacters.toString());
                        }

                        flushTemplateCharacters(nextCharacters);

                        stateChange(XML, NONE);
                        processQueue();

                        break;
                    default:
                }
        }
    }

    private char cleanQuote(char c) {
        if ((int) c == 8216 || (int) c == 8217 || (int) c == 8218) {
            c = '\'';
        }
        if ((int) c == 171 || (int) c == 187 || (int) c == 8220 || (int) c == 8221 || (int) c == 8222) {
            c = '"';
        }
        return c;
    }

    private void updateBlock(String island) {
        String command = extractCommand(island);
        if (blockEnd.contains(command)) {
            popBlock();
        }
        if (blockStart.contains(command)) {
            pushBlock();
        }
    }

    static private Pattern commandPattern = Pattern.compile("\\{%\\W*(\\w+)(\\W|%)?.*");

    static protected String extractCommand(String island) {
        Matcher m = commandPattern.matcher(island);
        if (!m.matches()) {
            return "";
        }

        return m.group(1);
    }


    private void flushXmlCharacters(StringBuffer characters) {
        if (onFlushXmlCharacters != null) {
            onFlushXmlCharacters.accept(characters.toString());
        }
        characters.delete(0, characters.length());
    }

    private void flushTemplateCharacters(StringBuffer characters) {
        if (onFlushTemplateCharacters != null) {
            onFlushTemplateCharacters.accept(characters.toString());
        }
        characters.delete(0, characters.length());
    }

    private void stateChange(ParserState state) throws XMLStreamException {
        stateChange(state, tagType);
    }

    private void stateChange(ParserState newState, TagType newTagType) throws XMLStreamException {
        this.state = newState;
        this.tagType = newTagType;
    }

    private void processQueue() {
        if (onProcessQueue != null) {
            onProcessQueue.run();
        }
    }


}
