package com.proxeus.xml.template.parser;

import javax.xml.stream.XMLStreamException;
import java.util.function.Consumer;

public interface TemplateParser {
    ParserState getState();

    TagType getTagType();

    int getBlockId();

    void onFlushCharacters(Consumer<CharactersEvent> onFlushCharacters);

    void onProcessQueue(Consumer<StateChangeEvent> onProcessQueue);

    void process(String characters) throws XMLStreamException;
}
