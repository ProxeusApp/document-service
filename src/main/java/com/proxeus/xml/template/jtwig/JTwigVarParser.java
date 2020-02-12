package com.proxeus.xml.template.jtwig;

import com.proxeus.xml.template.TemplateVarParser;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * JTwigVarParser parses all kind of vars provided in output blocks like {{...}} or code blocks like {%...%}.
 * It ensures the natural alphabetic order in the returned set and it is possible to filter variables by a prefix.
 */
public class JTwigVarParser implements TemplateVarParser {
    //use TreeSet to deliver sorted vars
    private Set<String> vars = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private String prefix = null;
    private static char[] varDivider = new char[]{',', '=', '*', '/', '%', '+', '-', '~', '<', '>', '!', '|', '?', ':', '(', ')', '[', ']', '{', '}'};
    private static Set<String> reservedWords;
    private static Set<String> reservedWordsStartsWith;

    static {
        reservedWords = new HashSet<>();
        add("true");
        add("false");
        add("in");
        add("as");
        add("null");
        add("is");
        add("not");
        add("with");
        add("and");
        add("or");
        reservedWordsStartsWith = new HashSet<>();
        addStartsWith("macros.");
    }

    private static void add(String reserved) {
        reservedWords.add(reserved);
    }

    private static void addStartsWith(String reserved) {
        reservedWordsStartsWith.add(reserved);
    }

    public JTwigVarParser(String varPrefix) {
        if (varPrefix != null && varPrefix.length() > 0) {
            this.prefix = varPrefix;
        }
    }

    @Override
    public Set<String> getVars() {
        return vars;
    }

    /**
     * parse code for vars
     *
     * @param content must either start with {{ .. }} or with {% .. %}
     */
    @Override
    public void parse(String content) {
        char currentChar = 0;
        char nextChar = 0;
        boolean insideString = false;
        char openedStringWith = 0;
        boolean ready = false;
        int size = content.length();
        int varStart = -1;
        int pipe = -1;//to skip filter names
        int dividerRes = -1;//-1 = no divider or whitespace, 0 = divider char, 1 = whitespace
        boolean inlineCondition = false; //?
        for (int i = 0; i < size; ++i) {
            currentChar = content.charAt(i);
            if (i + 1 < size) {
                nextChar = content.charAt(i + 1);
            } else {
                nextChar = 0;
            }
            if (ready) {
                if (!insideString && Character.isLetter(currentChar)) {
                    if (varStart == -1) {
                        varStart = i;
                    }
                } else if (!insideString && currentChar == '?') {
                    inlineCondition = true;
                    //var | filter divider
                } else if (currentChar == '\'' || currentChar == '"') {
                    if (insideString) {
                        if (currentChar == openedStringWith) {
                            insideString = false;
                        }
                    } else {
                        insideString = true;
                        openedStringWith = currentChar;
                        varStart = -1;
                    }
                    //var | filter divider
                } else if (!insideString && currentChar == '|') {
                    pipe = i;
                    //var divider
                } else if (!insideString && (dividerRes = isDivider(currentChar)) > -1) {
                    if (dividerRes == 1) {
                        continue;
                    }
                    if (currentChar == '(') {
                        //method name, skip it
                    } else if (!inlineCondition && currentChar == ':') {
                        //name inside object, skip it
                    } else if (varStart > -1) {
                        if (pipe > -1) {
                            //looks like a filter method
                            pipe = -1;
                        } else {
                            //ensure we close the inline condition
                            if (currentChar == ':') {
                                inlineCondition = false;
                            }
                            //found a var('s)
                            String[] varArr = content.substring(varStart, i).trim().split(" ");
                            for (String var : varArr) {
                                var = var.trim();
                                if (var.length() > 0 && isNotReserved(var)) {
                                    if (prefix != null) {
                                        if (var.startsWith(prefix)) {
                                            vars.add(var);
                                        }
                                    } else {
                                        vars.add(var);
                                    }
                                }
                            }
                        }
                    }
                    varStart = -1;
                } else if (!insideString && (Character.isSpaceChar(currentChar) || Character.isWhitespace(currentChar))) {
                    varStart = -1;
                }
            } else {
                if (currentChar == '{' && nextChar == '{') {
                    //output.. must include vars
                    ready = true;
                    ++i;
                    continue;
                } else if (currentChar == '{' && nextChar == '%') {
                    int nameStart = -1;
                    for (int c = i + 2; c < size; ++c) {
                        currentChar = content.charAt(c);
                        if (Character.isSpaceChar(currentChar) || Character.isWhitespace(currentChar)) {
                            if (nameStart > -1) {
                                i = c;
                                ready = true;
                                break;
                            }
                        } else if (nameStart == -1 && Character.isLetter(currentChar)) {
                            nameStart = c;
                        }
                    }
                    if (ready) {
                        continue;
                    }
                }
                return;
            }
        }
    }

    private boolean isNotReserved(String var) {
        if (reservedWords.contains(var)) {
            return false;
        }
        for (String s : reservedWordsStartsWith) {
            if (var.startsWith(s)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param c currect char
     * @return 1 = whitespace char, 0 divider char, -1 no whitespace or divider char
     */
    private int isDivider(char c) {
        if (Character.isSpaceChar(c) || Character.isWhitespace(c)) {
            return 1;
        }
        for (char d : varDivider) {
            if (d == c) {
                return 0;
            }
        }
        return -1;
    }


}
