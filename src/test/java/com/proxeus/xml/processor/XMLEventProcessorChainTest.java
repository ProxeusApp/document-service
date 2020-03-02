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
public class XMLEventProcessorChainTest {

    private List<String> test;
    private XMLEventFactory eventFactory;

    public XMLEventProcessorChainTest(List<String> test) {
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

            XMLEventProcessorChain p = new XMLEventProcessorChain(
                    new NoOpEventProcessor(),
                    new NoOpEventProcessor()
            );

            p.process(in, out);

            List<String> result = new ArrayList<>();
            while (out.hasNext()) {
                result.add(out.nextEvent().asCharacters().getData());
            }
            Assert.assertArrayEquals(test.toArray(), result.toArray());
        } catch (Exception e) {
            Assert.fail();
        }

    }

    @Test
    public void testEmpty() {
        try {
            XMLEventBuffer in = new XMLEventBuffer();
            XMLEventBuffer out = new XMLEventBuffer();

            for (String s : test) {
                in.add(eventFactory.createCharacters(s));
            }

            XMLEventProcessorChain p = new XMLEventProcessorChain();

            p.process(in, out);

            List<String> result = new ArrayList<>();
            while (out.hasNext()) {
                result.add(out.nextEvent().asCharacters().getData());
            }
            Assert.assertArrayEquals(test.toArray(), result.toArray());
        } catch (Exception e) {
            Assert.fail();
        }

    }

    @Test
    public void testHierarchy() {
        try {
            XMLEventBuffer in = new XMLEventBuffer();
            XMLEventBuffer out = new XMLEventBuffer();

            for (String s : test) {
                in.add(eventFactory.createCharacters(s));
            }

            XMLEventProcessorChain p = new XMLEventProcessorChain(new XMLEventProcessorChain());

            p.process(in, out);

            List<String> result = new ArrayList<>();
            while (out.hasNext()) {
                result.add(out.nextEvent().asCharacters().getData());
            }
            Assert.assertArrayEquals(test.toArray(), result.toArray());
        } catch (Exception e) {
            Assert.fail();
        }

    }
}
