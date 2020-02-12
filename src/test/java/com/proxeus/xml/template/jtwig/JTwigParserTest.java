package com.proxeus.xml.template.jtwig;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class JTwigParserTest {

    @Test
    public void extractCommandTest() {
        // [input, expected]
        String[][] tests = new String[][]{
                {"1", "", ""},
                {"2", "{% if", "if"},
                {"3", "{% if ", "if"},
                {"4", "{%if ", "if"},
                {"5", "{%if %} ", "if"},
                {"6", "{%if%} ", "if"},

        };

        Arrays.asList(tests).forEach(test ->
                Assert.assertEquals(test[0], test[2], JTwigParser.extractCommand(test[1]) ));
    }
}
