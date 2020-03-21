package com.proxeus.xml.template.parser;

public enum ParserState {
    //External states
    XML,
    MAYBE_DELIMITER,  // Either MAYBE_START_DELIMITER or MAYBE_END_DELIMITER
    TEMPLATE,
    // internal states
    MAYBE_START_DELIMITER,
    MAYBE_END_DELIMITER,
    SINGLE_QUOTE_STRING,
    DOUBLE_QUOTE_STRING,
}
