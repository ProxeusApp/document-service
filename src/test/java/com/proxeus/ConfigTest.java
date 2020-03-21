package com.proxeus;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ConfigTest {

    @Test
    public void create_shouldSetProperAttributesByPassingTheCorrectStructure() {
        Map<String, Object> map = new HashMap<>();
        map.put("protocol", "anyProtocol");
        map.put("host", "anyHost");
        map.put("tmpFolder", "anyFolder");
        map.put("port", 8081);

        final Config config = Config.create(map);

        assertEquals("anyProtocol", config.getProtocol());
        assertEquals("anyHost", config.getHost());
        assertEquals("anyFolder", config.getTmpFolder());
        assertEquals(8081, config.getPort().intValue());
    }

    @Test
    public void parseDurationToMillis_shouldConvertCorrectly() {
        final Map<String, Long> expectedResponsesMap = new HashMap<>();
        expectedResponsesMap.put("2s", 2000l);
        expectedResponsesMap.put("500s", 500000l);
        expectedResponsesMap.put("1m", 60000l);
        expectedResponsesMap.put("1h", 3600000l);
        expectedResponsesMap.put("24h", 86400000l);

        for(String input : expectedResponsesMap.keySet()) {
            long response = Config.parseDurationToMillis(input);
            long expected = expectedResponsesMap.get(input);

            assertEquals(expected, response);
        }
    }

    @Test(expected = RuntimeException.class)
    public void parseDurationToMillis_shouldReturnRuntimeExceptionOnUnknownSyntax() {
        Config.parseDurationToMillis("no-syntax");
    }
}