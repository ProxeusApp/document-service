package com.proxeus.xml;

import java.util.Set;

/**
 * Measurement helps to find the best way to the best suited nodes.
 */
public class Measurement {
    int distance = 0;
    Node node;
    boolean isXMLThatNeedsToBeWrapped;

    protected Measurement() {
    }

    protected void measureDistanceOutsideDown(Node target, Set<String> xmlTagNames, int maxRange) {
        node = target;
        while (maxRange > 0 && !isXMLThatNeedsToBeWrapped && node.parent != null && node.nextSibling() == null) {
            --maxRange;
            nextStep(node.parent, xmlTagNames.contains(node.name()));
        }
        if (target.start != null) {
            //try to move to the next sibling as it is a start tag
            Element element = node.nextSibling();
            if (maxRange > 0 && element != null && element instanceof Node && xmlTagNames.contains(((Node) element).name())) {
                nextStep((Node) element, true);
            }
        }
    }

    protected void measureDistanceOutsideUp(Node target, Set<String> xmlTagNames, int maxRange) {
        node = target;
        while (maxRange > 0 && !isXMLThatNeedsToBeWrapped && node.prevSibling() == null) {
            if (node.parent != null) {
                --maxRange;
                nextStep(node.parent, xmlTagNames.contains(node.name()));
            }
        }
    }

    protected void measureDistanceInsideDown(Node target, Set<String> xmlTagNames, int maxRange) {
        node = target;
        Element element;
        while (maxRange > 0 &&
                !isXMLThatNeedsToBeWrapped &&
                ((element = node.firstChild()) != null || (element = node.nextSibling()) != null) &&
                element.type == ElementType.XML) {
            --maxRange;
            nextStep((Node) element, xmlTagNames.contains(node.name()));
        }
    }

    protected void measureDistanceInsideUp(Node target, Set<String> xmlTagNames, int maxRange) {
        node = target;
        Element element;
        if (maxRange > 0 &&
                !isXMLThatNeedsToBeWrapped &&
                (element = node.prevSibling()) != null &&
                element.type == ElementType.XML) {
            --maxRange;
            nextStep((Node) element, xmlTagNames.contains(node.name()));
            while (maxRange > 0 &&
                    !isXMLThatNeedsToBeWrapped &&
                    node.hasStartAndEndTag() &&
                    (element = node.lastChild()) != null &&
                    element.type == ElementType.XML) {
                --maxRange;
                nextStep((Node) element, xmlTagNames.contains(node.name()));
            }
        }
    }

    private void nextStep(Node n, boolean isOneOfOurTargets) {
        ++distance;
        node = n;
        isXMLThatNeedsToBeWrapped = isOneOfOurTargets;
    }

    /**
     * help gc
     */
    protected void free() {
        node = null;
    }
}