package com.proxeus.xml.jtwig;

public enum ExtractorState {
    XML,
    MAYBE_BEGIN_ISLAND,
    ISLAND,
    SINGLE_QUOTE_STRING,
    DOUBLE_QUOTE_STRING,
    MAYBE_END_ISLAND,
}
