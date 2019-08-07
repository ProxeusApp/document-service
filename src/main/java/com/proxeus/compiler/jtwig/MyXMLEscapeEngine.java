package com.proxeus.compiler.jtwig;

import org.apache.commons.text.StringEscapeUtils;
import org.jtwig.escape.EscapeEngine;

public class MyXMLEscapeEngine implements EscapeEngine {
    private static final MyXMLEscapeEngine instance = new MyXMLEscapeEngine();

    public static MyXMLEscapeEngine instance () {
        return instance;
    }

    private MyXMLEscapeEngine() {}

    @Override
    public String escape(String input) {
        return StringEscapeUtils.escapeXml11(input);
    }
}