package com.proxeus.xml.template.parser;

public class CharactersEvent extends StateChangeEvent {
    private String characters;
    private ParserState state;
    private TagType tagType;

    public CharactersEvent(String characters, ParserState state, TagType tagType, int blockId) {
        super(state, tagType, blockId);
        this.characters = characters;
    }

    public String getCharacters() {
        return characters;
    }
}
