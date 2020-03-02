package com.proxeus.xml;

import org.junit.Test;

import static com.proxeus.xml.CodeType.CodeBlock;
import static com.proxeus.xml.CodeType.Output;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CodeTest {

    @Test
    public void constructor_shouldInitializeRelations() {
        Node node1 = mock(Node.class);
        Node node2 = mock(Node.class);

        Code code = new Code(CodeBlock, node1, node2);

        assertEquals(2, code.getRelations().size());
        assertEquals(node1, code.getRelations().get(0));
        assertEquals(node2, code.getRelations().get(1));
    }

    @Test
    public void prepend_shouldPrependCorrectly() {
        Node node1 = mock(Node.class);
        Node node2 = mock(Node.class);
        Node node3 = mock(Node.class);

        Code code = new Code(CodeBlock, node1, node2);
        code.prepend(node3);

        assertEquals(3, code.getRelations().size());
        assertEquals(node3, code.getRelations().get(0));
    }

    @Test
    public void haveTheSameParent_shouldReturnFalseWithNoRelations() {
        Code code = new Code(CodeBlock);
        boolean response = code.haveTheSameParent();

        assertFalse(response);
    }

    @Test
    public void haveTheSameParent_shouldReturnTrueWithTwoNodesWithSameParent() {
        Node node1 = mock(Node.class);
        Node node2 = new Node(new Tag());
        node2.parent = node1;
        Node node3 = new Node(new Tag());
        node3.parent = node1;

        Code code = new Code(CodeBlock, node2, node3);
        boolean response = code.haveTheSameParent();

        assertTrue(response);
    }

    @Test
    public void isMacroBlock_shouldReturnFalseWithNoRelations() {
        Code code = new Code(CodeBlock);
        boolean response = code.isMacroBlock();

        assertFalse(response);
    }

    @Test
    public void isMacroBlock_shouldReturnFalseWithNoCodeBlock() {
        Code code = new Code(Output);
        boolean response = code.isMacroBlock();

        assertFalse(response);
    }

    @Test
    public void isMacroBlock_shouldReturnFalseWithCodeBlockButNoMacroAsFirstNode() {
        Node node1 = mock(Node.class);
        when(node1.name()).thenReturn("anything-but-no-macro");

        Code code = new Code(CodeBlock, node1);
        boolean response = code.isMacroBlock();

        assertFalse(response);
    }

    @Test
    public void isMacroBlock_shouldReturnFalseWithCodeBlockAndMacroAsSecondNode() {
        Node node1 = mock(Node.class);
        when(node1.name()).thenReturn("anything-but-no-macro");
        Node node2 = mock(Node.class);
        when(node2.name()).thenReturn("macro");

        Code code = new Code(CodeBlock, node1);
        boolean response = code.isMacroBlock();

        assertFalse(response);
    }

    @Test
    public void isMacroBlock_shouldReturnTrueWithCodeBlockAndMacroAsFirstNode() {
        Node node1 = mock(Node.class);
        when(node1.name()).thenReturn("macro");

        Code code = new Code(CodeBlock, node1);
        boolean response = code.isMacroBlock();

        assertTrue(response);
    }
}