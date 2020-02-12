package com.proxeus.xml.template;

import java.util.Set;

public interface TemplateVarParser {
    void parse(String input);
    Set<String> getVars();
}
