package com.proxeus.xml;

/**
 * We differentiate only between XML, CODE or TEXT
 * XML is everything that starts with < and ends with > like <body/>
 * CODE is {% .. %}, {{ .. }} or {# .. #}
 * TEXT is everything else
 */
public enum ElementType {XML, CODE, TEXT}