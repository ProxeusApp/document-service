package com.proxeus.xml;

public enum CodeType {
    NoCode,
    //{% ... %}
    CodeBlock,
    //{{ ... }}
    Output,
    //{# ... #}
    Comment;

    public boolean isCode(){
        return this != NoCode;
    }
    public boolean isNoCode(){
        return this == NoCode;
    }
}