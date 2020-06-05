package com.proxeus.xml.template.parser;

import static com.proxeus.xml.template.parser.ParserState.*;

public class StateChangeEvent {
    private ParserState state;
    private TagType tagType;
    private int blockId;

    public StateChangeEvent(ParserState state, TagType tagType, int blockId) {
        this.state = state;
        this.tagType = tagType;
        this.blockId = blockId;
    }

    public ParserState getState() {
        if (state == MAYBE_START_DELIMITER || state == MAYBE_END_DELIMITER){
            return MAYBE_DELIMITER;
        }

        if (state == STRING){
            return TEMPLATE;
        }

        return state;
    }

    public TagType getTagType() {
        return tagType;
    }
}
