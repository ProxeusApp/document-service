package com.proxeus.xml;

public enum TagType {
    START, //<body> or code {% if %}
    END, //</body> or code {% endif %}
    START_AND_END, //<title/> or code {% set %}
    HEADER, //<?xml...?>
    TEXT, //everything else
}