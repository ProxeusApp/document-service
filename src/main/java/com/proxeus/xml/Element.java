package com.proxeus.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Element is the base class of Node and it represents either XML, code or text.
 */
public class Element {
    public List<Element> children;
    public ElementType type;
    public Tag start;
    public Tag end;
    public Node parent;
    public int depth;

    public Element() {}

    // this constructor is only used for text
    public Element(Tag start){
        this.start = start;
        type = ElementType.TEXT;
    }

    /**
     * Create an end tag out of the start tag.
     */
    public void createEndTag() {
        setEnd(start.createEndTag());
    }

    /**
     * Create a start tag out of the end tag.
     */
    public void createStartTag() {
        setStart(end.createStartTag());
    }

    /**
     * Remove this element only and move the children to the parent at the same position.
     */
    public void removeThisElementButKeepTheChildren(){
        if(hasChildren()){
            int indexOfMe = parent.children.indexOf(this);
            parent.addChildsAfter(indexOfMe, children);
        }
        remove();
    }

    /**
     * Replace this with newNode.
     * If parent is null, it will do nothing.
     * Which means replacing the root node is not supported.
     */
    public void replaceWith(Element newElement){
        if(parent != null){
            int indexOfMe = parent.children.indexOf(this);
            parent.children.set(indexOfMe, newElement);
            newElement.parent = parent;
        }
    }

    /**
     * Just remove this element.
     */
    public void remove(){
        parent.children.remove(this);
    }

    /**
     * Set the start tag of this element.
     * @param s the start tag
     */
    public void setStart(Tag s) {
        start = s;
        if (type == null && start != null) {
            if (start.isCode()) {
                type = ElementType.CODE;
            } else {
                type = ElementType.XML;
            }
        }
    }

    /**
     * Set the end tag of this element.
     * @param e the end tag
     */
    public void setEnd(Tag e) {
        end = e;
        if (type == null && end != null) {
            if (end.isCode()) {
                type = ElementType.CODE;
            } else {
                type = ElementType.XML;
            }
        }
    }

    /**
     * Check whether this element has an end tag.
     * @return false it has an end tag otherwise true
     */
    public boolean hasNoEndTag() {
        if (start != null && start.type == TagType.START_AND_END) {
            return false;
        }
        return end == null;
    }

    /**
     * Check whether this element a start and end tag.
     * @return true if it has otherwise false
     */
    public boolean hasStartAndEndTag() {
        return start != null && end != null;
    }

    /**
     * Check whether this element has no start tag.
     * @return true if it doesn't otherwise false
     */
    public boolean hasNoStartTag() {
        return start == null;
    }

    /**
     * Check whether it has only an end tag.
     * @return true if it has otherwise false
     */
    public boolean isEndTagOnly(){
        return start == null;
    }

    /**
     * Get the previous sibling of this element but skip whitespace elements.
     * @return the previous element or @null if there was only whitespaces or no siblings
     */
    public Element prevSibling() {
        if(parent==null){
            return null;
        }
        int indexOfMe = parent.children.indexOf(this);
        Element e;
        do {
            --indexOfMe;
            if (indexOfMe < 0) {
                return null;
            }
            e = parent.children.get(indexOfMe);
        } while (e.isWhiteSpaceOnlyElement());
        return e;
    }

    /**
     * Next sibling without whitespace elements.
     * @return next sibling element
     */
    public Element nextSibling() {
        if(parent==null){
            return null;
        }
        int indexOfMe = parent.children.indexOf(this);
        return nextSiblingOfIndex(indexOfMe);
    }

    /**
     * Next sibling by the provided index.
     * @return the next element or null
     */
    public Element nextSiblingOfIndex(int indexOfMe) {
        if(parent==null){
            return null;
        }
        int s = parent.children.size();
        Element e;
        do {
            ++indexOfMe;
            if (indexOfMe == s) {
                return null;
            }
            e = parent.children.get(indexOfMe);
        } while (e.isWhiteSpaceOnlyElement());
        return e;
    }

    /**
     * Get the first child of this element but skip whitespace elements.
     * @return first child element or @null if there was only whitespaces or no children
     */
    public Element firstChild(){
        if(hasChildren()){
            Element e;
            for(int i = 0; i < children.size(); ++i){
                e = children.get(i);
                if(e.isWhiteSpaceOnlyElement()){
                    continue;
                }
                return e;
            }
        }
        return null;
    }

    /**
     * Get the last child of this element but skip whitespace elements.
     * @return last child element or @null if there was only whitespaces or no children
     */
    public Element lastChild(){
        if(hasChildren()){
            Element e;
            for(int i = children.size()-1; i > -1; --i){
                e = children.get(i);
                if(e.isWhiteSpaceOnlyElement()){
                    continue;
                }
                return e;
            }
        }
        return null;
    }

    /**
     * Check whether #onlyChild is the only child of this element but do not count whitespace elements in.
     * @param onlyChild the element that needs to be checked
     * @return true if it is the only child with or without whitespace elements
     */
    public boolean withoutWhitespacesTheOnlyChildIs(Node onlyChild) {
        if (hasChildren()) {
            int index = children.indexOf(onlyChild);
            if(index > -1){
                //onlyChild is really a child of this parent
                int count = 0;
                Iterator<Element> elementIterator = children.iterator();
                while (count < 2 && elementIterator.hasNext()) {
                    Element e = elementIterator.next();
                    if (e.isWhiteSpaceOnlyElement()) {
                        continue;
                    }
                    ++count;
                }
                if(count == 1){
                    //onlyChild is indeed the only child of this parent
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The depth level in the XML structure counting from root to the position of this element.
     * @param calculate true if it should be recalculated
     * @return depth level
     */
    public int getDepth(boolean calculate) {
        if(calculate){
            depth = -1;
        }
        return getDepth();
    }

    /**
     * The depth level in the XML structure counting from root to the position of this element.
     * @return depth level
     */
    public int getDepth() {
        if (depth <= -1) {
            depth = 1;
            if (parent != null) {
                for (Node p = parent; p != null; p = p.parent) {
                    ++depth;
                }
            }
        }
        return depth;
    }

    /**
     * The public method for finding elements recursively by name.
     * @param name of the element you are looking for
     * @return list of elements
     */
    public List<Element> findElementByName(String name) {
        ArrayList<Element> elements = new ArrayList<>(4);
        innerFindElementByName(elements, name);
        return elements;
    }

    /**
     * Find elements recursively by name.
     * @param list to be filled with elements that matches the name
     * @param name of the element you are looking for
     */
    private void findElementByName(List<Element> list, String name){
        if (start != null && name.equals(start.name)) {
            list.add(this);
        }
        innerFindElementByName(list, name);
    }

    /**
     * Helper method for findElementByName.
     */
    private void innerFindElementByName(List<Element> list, String name){
        if (hasChildren()) {
            Iterator<Element> elementIterator = children.iterator();
            while (elementIterator.hasNext()) {
                Element e = elementIterator.next();
                e.findElementByName(list, name);
            }
        }
    }

    /**
     * write attr value
     */
    public void attr(String attrName, String value) {
        if(start == null){
            return;
        }
        start.attr(attrName, value);
    }

    /**
     * read attr value
     */
    public String attr(String name){
        if(start == null){
            return null;
        }
        return start.attr(name);
    }

    /**
     * Does this element have children?
     * @return true if it does
     */
    public boolean hasChildren() {
        return children != null && children.size() > 0;
    }

    private final static Pattern whiteSpaceOnlyRegex = Pattern.compile("^\\s*$");
    /**
     * Is this element a whitespace only element?
     * @return true if it is
     */
    public boolean isWhiteSpaceOnlyElement() {
        return type == ElementType.TEXT && (start!=null && start.whiteSpacesOnly());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(length());
        toString(sb);
        return sb.toString();
    }

    public InputStream toInputStream(Charset charset){
        StringBuilder sb = new StringBuilder(length());
        toString(sb);
        return new ByteArrayInputStream(sb.toString().getBytes(charset));
    }

    public void toString(StringBuilder sb) {
        if (start != null) {
            start.toString(sb);
        }

        if (children != null && children.size() > 0) {
            Iterator<Element> elementIterator = children.iterator();
            while (elementIterator.hasNext()) {
                elementIterator.next().toString(sb);
            }
        }

        if (end != null) {
            end.toString(sb);
        }
    }

    public void toOutputStream(OutputStream os, Charset charset) throws IOException {
        if (start != null) {
            start.toStream(os, charset);
        }

        if (children != null && children.size() > 0) {
            Iterator<Element> elementIterator = children.iterator();
            while (elementIterator.hasNext()) {
                elementIterator.next().toOutputStream(os, charset);
            }
        }

        if (end != null) {
            end.toStream(os, charset);
        }
    }

    public int length(){
        int len = 0;
        if (start != null) {
            len += start.length();
        }
        if (children != null && children.size() > 0) {
            Iterator<Element> elementIterator = children.iterator();
            while (elementIterator.hasNext()) {
                len += elementIterator.next().length();
            }
        }
        if (end != null) {
            len += end.length();
        }
        return len;
    }
}
