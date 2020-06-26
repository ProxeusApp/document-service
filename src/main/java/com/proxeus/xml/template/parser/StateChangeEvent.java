package com.proxeus.xml.template.parser;

public class StateChangeEvent {
    private TagType tagType;

    public StateChangeEvent(ParserState state, TagType tagType, int blockId) {
        this.tagType = tagType;
    }


    public TagType getTagType() {
        return tagType;
    }
}
