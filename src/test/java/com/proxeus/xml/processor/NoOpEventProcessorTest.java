package com.proxeus.xml.processor;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.xml.stream.XMLEventFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(Parameterized.class)
public class NoOpEventProcessorTest {

    private List<String> test;
    private XMLEventFactory eventFactory;

    public NoOpEventProcessorTest(List<String> test) {
        this.test = test;
        this.eventFactory = XMLEventFactory.newInstance();
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<? extends Object> tests() {
        return Arrays.asList(
                Arrays.asList(),
                Arrays.asList("a", "b", "c"));
    }

    @Test
    public void test() {
        try {
            XMLEventBuffer in = new XMLEventBuffer();
            XMLEventBuffer out = new XMLEventBuffer();

            for (String s : test) {
                in.add(eventFactory.createCharacters(s));
            }

            NoOpEventProcessor p = new NoOpEventProcessor();
            p.process(in, out);


            List<String> result = new ArrayList<>();
            while (out.hasNext()) {
                result.add(out.nextEvent().asCharacters().toString());
            }
            Assert.assertArrayEquals(test.toArray(), result.toArray());
        } catch (Exception e) {
            Assert.fail();
        }

    }

}