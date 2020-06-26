package com.proxeus.xml.template.parser;

import javax.xml.stream.XMLStreamException;
import java.util.function.Consumer;

public interface TemplateParser {
    ParserState getState();

    TagType getTagType();

    int getBlockId();

    void onFlushXmlCharacters(Consumer<String> onFlushCharacters);

    void onFlushTemplateCharacters(Consumer<String> onFlushCharacters);

    void onProcessQueue(Runnable onProcessQueue);

    void process(String characters) throws XMLStreamException;
}
