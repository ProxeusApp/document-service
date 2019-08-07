package com.proxeus.xml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * A Node in a well formed XML contains the following:
 * <body> //start tag
 *     <span>a</span> //children
 *     <span>b</span> //children
 * </body> //end tag
 * or
 * <span/> //start tag with type START_AND_END, end and childs are null
 *
 * A Node contains code elements too, but does not keep track of start or end nor children entirely.
 * Each code element is kept in separated node. The children that are supposed to be the children of the code are the children of the code parent.
 * This way we gain performance when we structure the code well in the XML structure instead of dragging children along.
 * <body>
 *     {%if%} //#NodeIf start
 *     <span></span> //#NodeBody children
 *     {%endif%} //#NodeEndIf end
 * </body>
 */
public class Node extends Element {
    public boolean interestingForLoopBack = true;

    public Node(Tag s) {
        start = s;
        if (start != null) {
            if (start.codeType.isCode()) {
                type = ElementType.CODE;
            } else {
                type = ElementType.XML;
            }
        }
    }

    /**
     * Before this method can be called, cautious analysis must be made to ensure
     * the way is free without breaking the content
     */
    protected void wrapAround(Node toBeWrapped){
        if(toBeWrapped==null || (start!=null && end!=null)){
            //don't do anything if this node is not missing either start or end tag
            //as wrapping is only used for code
            return;
        }
        int newIndex = toBeWrapped.parent.children.indexOf(toBeWrapped);
        this.parent.children.remove(this);
        this.parent = toBeWrapped.parent;
        if(start != null && end == null){
            //wrap as start tag before the target
            toBeWrapped.parent.children.add(newIndex, this);
        }else if(start == null && end != null){
            //wrap as end tag after the target
            toBeWrapped.parent.children.add(newIndex+1, this);
        }
    }

    /**
     * @return the name of the XML tag or code.
     *
     * Example with XML tag <body> name = "body"
     * Example with code {% if %} name = "if"
     * Example with different code {{input.date}} name = ""
     */
    public String name() {
        return start != null ? start.name : (end != null ? end.name : null);
    }

    /**
     * Add one child to the bottom of this node.
     */
    public void addChild(Element e) {
        if (children == null) {
            children = new ArrayList<>(1);
        }
        e.parent = this;
        children.add(e);
    }

    /**
     * Add children to the bottom of this node.
     */
    public void addChilds(List<Element> children) {
        if (children != null && children.size() > 0) {
            if (this.children == null) {
                this.children = new ArrayList<>(1);
            }
            Iterator<Element> elementIterator = children.iterator();
            while (elementIterator.hasNext()) {
                Element e = elementIterator.next();
                e.parent = this;
                this.children.add(e);
            }
            children.clear();
        }
    }

    /**
     * Add children after @param n. @param n must be a child of this very node already.
     * If it is not a child of this node, nothing will happen.
     * If it is a child of this node, the children will be appended after @param n and the children list will be cleared.
     * @param n append after this child node
     * @param children to be appended
     */
    public void addChildsAfter(Node n, List<Element> children) {
        int eleSize;
        if (children != null && (eleSize = children.size()) > 0) {
            if (this.children == null) {
                this.children = new ArrayList<>(eleSize);
                return;
            }
            addChildsAfter(this.children.indexOf(n), children);
        }
    }

    /**
     * Add children after the provided index.
     * If index is in range, the children will be appended at this index and the children list will be cleared.
     * If index is -1 nothing will happen. If index is not childs.size() or smaller therefore not in range an exception will be thrown.
     * @param i append at this index
     * @param children to be appended
     */
    public void addChildsAfter(int i, List<Element> children) {
        if (children != null) {
            if (this.children == null) {
                this.children = new ArrayList<>(children.size());
            }
            if (i > -1) {
                ++i;
                Element e = null;
                for (int c = 0; c < children.size(); ++c) {
                    e = children.get(c);
                    e.parent = this;
                    this.children.add(i, e);
                    ++i;
                }
                children.clear();
            }
        }
    }

    /**
     * Removes empty XML wrappers like:
     * <body>
     *     <p> //will be removed as white spaces are ignored
     *         <span> //will be removed as white spaces are ignored
     *             {%if%}
     *         </span>
     *     </p>
     * </body>
     * If p and span are whitelisted to be removed in the config.
     * Outcome:
     * <body>
     *     {%if%}
     * </body>
     * If whitelistedTags is empty, nothing will happen.
     * @param whitelistedTags
     */
    public void removeEmptyXMLWrappers(Set<String> whitelistedTags) {
        while (parent != null && parent.parent != null && whitelistedTags.contains(parent.name()) && parent.withoutWhitespacesTheOnlyChildIs(this)) {
            --depth;
            int myNewIndex = parent.parent.children.indexOf(parent);
            parent.parent.children.set(myNewIndex, this);
            parent = parent.parent;
        }
    }

    /**
     * This method tries to move the target outside the parent without moving the any text elements.
     * By checking the previous siblings and next siblings.
     * If there is no way out, the XML patterns are reconstructed to free the way for the target.
     * @param commonDepth the depth level this method has to reach
     */
    public void moveOutsideToParent(int commonDepth) {
        while (depth > commonDepth) {
            if (parent != null && parent.parent != null) {
                //try to move outside down if it is end code
                if(type == ElementType.CODE && start == null && nextSibling() == null){
                    moveOutsideDown();
                    continue;
                }
                if (prevSibling() == null) {
                    moveOutsideUp();
                } else {
                    moveOutsideDown();
                }
            }
        }
    }

    /**
     * This method should be called only if there are no previous siblings
     * and the parent of the current parent is not null/root.
     *
     * 1. add children to the parent after the target if there are any
     * 2. replace target with the parents position
     * 3. get all the children from that position
     */
    public void moveOutsideUp() {
        int indexOfMe = parent.children.indexOf(this);
        if(hasChildren()) {
            parent.addChildsAfter(indexOfMe, children);
        }
        parent.children.remove(this);
        Node oldPp = parent.parent;
        int indexOfParent = oldPp.children.indexOf(parent);
        oldPp.children.add(indexOfParent, this);
        parent = oldPp;
        --depth;
        int si = oldPp.children.size();
        if(start != null && start.isNoCode() && start.type == TagType.START && si-1 > indexOfParent){
            children = new ArrayList<>(Math.max(si, indexOfParent) - Math.min(si, indexOfParent));
            ListIterator<Element> li = oldPp.children.listIterator(indexOfParent);
            Element e = null;
            while (li.hasNext()) {
                e = li.next();
                e.parent = this;
                --e.depth;
                children.add(e);
                li.remove();
            }
        }
    }

    /**
     * 1. if has children or siblings -> new Node with a copy of the parent containing all children or siblings of the target
     * 2. if it has children or siblings -> move target down and set the new Node as the child of the target or child of the targets parent, otherwise just move outside down
     */
    public void moveOutsideDown() {
        int i = parent.children.indexOf(this);
        if (i > -1) {
            if (hasChildren()) {
                parent.children.remove(i);
                Tag newStartTag = parent.start.copy();
                Node n = new Node(newStartTag);
                n.setEnd(newStartTag.createEndTag());
                n.addChilds(children);
                addChild(n);
                i = parent.parent.children.indexOf(parent) + 1;
            } else if (nextSiblingOfIndex(i) != null) {
                parent.children.remove(i);
                int si = parent.children.size();
                ArrayList<Element> nextSiblings = new ArrayList<>(Math.max(si, i) - Math.min(si, i));
                ListIterator<Element> li = parent.children.listIterator(i);
                Tag newStartTag = parent.start.copy();
                Node n = new Node(newStartTag);
                n.setEnd(newStartTag.createEndTag());
                Element e = null;
                while (li.hasNext()) {
                    e = li.next();
                    e.parent = n;
                    nextSiblings.add(e);
                    li.remove();
                }
                n.children = nextSiblings;
                i = parent.parent.children.indexOf(parent) + 1;
                parent.parent.children.add(i, n);
                n.parent = parent.parent;
            } else {
                parent.children.remove(i);
                i = parent.parent.children.indexOf(parent) + 1;
            }
            parent.parent.children.add(i, this);
            parent = parent.parent;
            --depth;
        }
    }

    /**
     * move children to the parent at the same position if there are any children
     */
    public void moveChildsToParentAtTheSamePosition(){
        if (hasChildren()) {
            //move the children to the parent and clear the childs list of this node
            parent.addChildsAfter(this, children);
            //set it to failed as we are not going to lookup for an end tag for this node anymore
            interestingForLoopBack = false;
        }
    }
}
