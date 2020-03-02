package com.proxeus.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Code is responsible for the correct relations and meaningful placement of code nodes.
 */
public class Code {
    protected boolean isComment;
    private CodeType codeType;
    private List<Node> relations;
    private boolean needsToMatchParentExactly = false;

    protected Code(CodeType codeType, Node...n) {
        this.codeType = codeType;
        relations = new ArrayList<>(n.length);
        for(Node node : n){
            relations.add(node);
        }
    }

    /**
     * Prepend code relation is needed for the main loop back.
     * As we start with the end of a block.
     * @param code related to the once we already have in relations
     */
    protected void prepend(Node code){
        relations.add(0, code);
    }

    /**
     * Check whether we have the same parent on all codes.
     * @return true if the parents are the same otherwise false
     */
    protected boolean haveTheSameParent(){
        if(relations.size() == 0){
            return false;
        }

        Node p = relations.get(0);
        p = p.parent;

        for (Node loopNode : relations) {
            if(loopNode == null || p != loopNode.parent){
                return false;
            }
        }
        return true;
    }

    /**
     * Check wether it is a macro block like {% macro ...%}
     */
    protected boolean isMacroBlock(){
        if(relations.isEmpty()) {
            return false;
        }

        Node firstRelation = relations.get(0);
        if(firstRelation.name() == null) {
            return false;
        }

        return codeType == CodeType.CodeBlock && firstRelation.name().equals("macro");
    }

    /**
     * To support simple macro usage by {{macros.function...}}
     * and to recognize it via *macros.* so we can remove the empty wrappers safely on {{..}} too
     * the removal is important to support inline paragraph templates
     **/
    protected void doMacroImport(){
        if(relations.size()==2){
            Node endMacro = relations.get(1);
            int indexOfEndMacro = endMacro.parent.children.indexOf(endMacro);
            StringBuffer sb = new StringBuffer("{% import _self as macros %}");
            Node macroNode = new Node(new Tag(CodeType.CodeBlock, sb, 0, sb.length(), TagType.START_AND_END));
            endMacro.parent.children.add(indexOfEndMacro+1, macroNode);
        }
    }

    private final static Pattern macroAndBlockUsageRegex = Pattern.compile("\\{\\{\\s*(macros\\.\\w+|block\\s*\\().*");
    /**
     * Check whether we should remove empty XML wrappers around this code.
     * Usually on output code like {{ ... }}, we do not remove wrappers as it would lead to unexpected content behaviour.
     * But on blocks like {% if .. %} or {# comment #} we can remove it as long as there is no content between the parent and this code.
     * Whitespace count as no content.
     */
    protected boolean shouldRemoveEmptyXMLWrappersAroundThisCode(){
        return codeType==CodeType.CodeBlock || (codeType == CodeType.Output && macroAndBlockUsageRegex.matcher(relations.get(0).start.toCharSequence()).matches());
    }

    protected Node findTheCommonParent() {
        List<Node> relatedNodes = new ArrayList<>(relations.size());
        relatedNodes.addAll(relations);
        needsToMatchParentExactly = true;
        findNextCommonDepthLevel(relatedNodes);
        return relatedNodes.get(0);
    }

    /**
     * Find the next common depth level to be able to move code suitable.
     * @param config provides a code type specific setting.
     *               In case the depth level is not enough and the pieces are expected to be on the same parent.
     * @return depth level
     */
    protected int findNextCommonDepthLevel(Config config) {
        //create a new list so we can modify the entries if necessary to find the common parent
        List<Node> relatedNodes = new ArrayList<>(relations.size());
        relatedNodes.addAll(relations);
        needsToMatchParentExactly = config.NeedsToMatchParentExactly(this);
        return findNextCommonDepthLevel(relatedNodes);
    }

    protected int findNextCommonDepthLevel(List<Node> relatedNodes){
        //calculate depth
        int i = 0;
        int c = relatedNodes.size();
        Node p;
        int min = Integer.MAX_VALUE;
        int max = -1;

        int[] depths = new int[c];
        for (; i < c; ++i) {
            p = relatedNodes.get(i);
            //not handling the last lvl before root
            //but so far it is anyway not needed
            if (p.parent != null) {
                if (max < p.getDepth(true)) {
                    max = p.depth;
                }
                if (min > p.depth) {
                    min = p.depth;
                }
                depths[i] = p.depth;
                p = p.parent;
                relatedNodes.set(i, p);
            }
        }

        if(min == max && matchesParentOrTheSameType(relatedNodes)){
            /**
             * If parent matches correctly.
             *
             *  -------------------OR------------------------
             *
             * If parent is just of the same type and on the same depth level.
             *
             * It is still cutting but not across, it will lead to a valid structure.
             *
             *  Example:
             *  <a>before if >>>|{if false} after if</a>
             *  <a>before else..{else}|<<<after else</a>
             *
             *  Outcome:
             *  <a>before if >>>||<<<after else</a>
             */
            return -1;
        }

        /**
         * give it a try with the current min depth and keep trying until 0
         * if the common depth didn't provide the same parent type
         */
        for(;min>0;--min){
            //use the closest depth to find the common parent or the parent with the same depth level and type
            for (i = 0; i < relatedNodes.size(); ++i) {
                if (min < depths[i]) {
                    int diff = depths[i] - min;
                    p = relatedNodes.get(i);
                    for (int d = 0; d < diff; ++d) {
                        if (p.parent != null) {
                            p = p.parent;
                            --depths[i];
                            if(min>depths[i]){
                                min = depths[i];
                            }
                        }
                    }
                    relatedNodes.set(i, p);
                }
            }

            if(sameDepths(depths) && matchesParentOrTheSameType(relatedNodes)){
                return depths[0];
            }
        }
        return -1;
    }

    /**
     * @param depths check whether the sequence contains the exact same value on all the entries
     * @return true if all values are the same else false
     */
    private boolean sameDepths(int[] depths) {
        int firstDepth = depths[0];
        for(int i = 1; i < depths.length; ++i){
            if(firstDepth!=depths[i]){
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if this code needs to match parent exactly otherwise of the same parent type.
     * @param relatedNodes containing the parents
     * @return true if all the parents in the list are the exact same or of the same type
     */
    private boolean matchesParentOrTheSameType(List<Node> relatedNodes) {
        if(needsToMatchParentExactly){
            return matchesParentOnAll(relatedNodes);
        }
        return sameParentTypeOnAll(relatedNodes);
    }

    /**
     * The parent is exactly the same.
     */
    private boolean matchesParentOnAll(List<Node> relatedNodes) {
        Node p = relatedNodes.get(0);
        for (int i = 1; i < relatedNodes.size(); ++i) {
            if (relatedNodes.get(i) != p) {
                return false;
            }
        }
        return true;
    }

    /**
     * The parent could be the exact same or just of the same type.
     * Same type means it all have the same tag name.
     */
    private boolean sameParentTypeOnAll(List<Node> relatedNodes) {
        Node p = relatedNodes.get(0);
        for (int i = 1; i < relatedNodes.size(); ++i) {
            if (!relatedNodes.get(i).name().equals(p.name())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Try to find the best suited tags for this code to wrap.
     * @param xmlTagNames the configured and best suited tags
     * @param maxRange when to give up for end and start code
     */
    protected void tryToWrapXMLTag(Set<String> xmlTagNames, int maxRange) {
        //The one with the shortest distance will be chosen in case there are many.
        Measurement[] ms = new Measurement[]{new Measurement(), new Measurement(), new Measurement()};
        ms[0].measureDistanceOutsideUp(relations.get(0), xmlTagNames, maxRange);
        ms[1].measureDistanceOutsideDown(relations.get(0), xmlTagNames, maxRange);
        ms[2].measureDistanceInsideDown(relations.get(0), xmlTagNames, maxRange);
        Arrays.sort(ms, (left, right) -> {
            if(left.isXMLThatNeedsToBeWrapped && (left.distance<right.distance || !right.isXMLThatNeedsToBeWrapped)){
                return -1;
            }else{
                return 1;
            }
        });
        for(int i = 0; i < ms.length; ++i){
            if(ms[i].isXMLThatNeedsToBeWrapped){
                if(isXMLWrappedByCode(ms[i],maxRange)){
                    break;
                }
            }else{
                break;
            }
        }
        freeMeasurements(ms);
    }

    /**
     * to make it easier for the gc
     */
    private void freeMeasurements(Measurement[] ms){
        for(int i = 0; i < ms.length; ++i){
            ms[i].free();
        }
    }

    /**
     * This method takes the measurement of the start code and tries to measure the end code.
     * If that works out, it takes care of the wrapping otherwise it returns false.
     *
     * @param m keeps important data of the trial
     * @param maxRange when to give up for the end code
     * @return true if successfully wrapped otherwise false
     */
    private boolean isXMLWrappedByCode(Measurement m, int maxRange){
        Node startCode = relations.get(0);
        Node endCode = relations.get(1);
        int depthOfXMLToBeWrapped = m.node.getDepth(true);
        Element prev = endCode.prevSibling();
        if(prev != null && (m.node == prev || depthOfXMLToBeWrapped == prev.getDepth(true))){
            //wrap start code only as end code seems to be correct already
            startCode.wrapAround(m.node);
            return true;
        }
        int depthOfEndCode = endCode.getDepth(true);
        Set<String> xmlTagName = new HashSet<>(1);
        xmlTagName.add(m.node.name());
        Measurement endCodeMeasurement = new Measurement();
        if(depthOfXMLToBeWrapped < depthOfEndCode){
            endCodeMeasurement.measureDistanceOutsideDown(endCode, xmlTagName, maxRange);
        }else{
            endCodeMeasurement.measureDistanceInsideUp(endCode, xmlTagName, maxRange);
        }
        if(endCodeMeasurement.isXMLThatNeedsToBeWrapped){
            if(m.node == endCodeMeasurement.node || depthOfXMLToBeWrapped == endCodeMeasurement.node.getDepth(true)){
                //wrap start code
                startCode.wrapAround(m.node);
                //wrap end code
                endCode.wrapAround(endCodeMeasurement.node);
                return true;
            }
        }
        return false;
    }

    /**
     * As code will be written as document content, it gets messed up.
     * This method is the initial method that ensures it gets moved to the next suitable and meaningful place.
     */
    protected void moveToCommonParent(Config config) {
        moveOutsideToParent(findNextCommonDepthLevel(config));
    }

    /**
     * With the common depth level and the related code nodes we can no start to try our way to place them meaningful.
     * @param commonDepth of all the nodes in relations
     */
    protected void moveOutsideToParent(int commonDepth) {
        if (commonDepth<=-1) {
            return;
        }
        for(Node n : relations){
            if (n.depth > commonDepth) {
                n.moveOutsideToParent(commonDepth);
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    /**
     * Assuming all code relations have the same parent otherwise it leads to unexpected behaviours.
     */
    public void toString(StringBuilder sb) {
        Node lastRelatedNode = null;
        for(Node codeNode : relations){
            if(lastRelatedNode!=null){
                if(lastRelatedNode.parent!= null){
                    int indexOfMe = lastRelatedNode.parent.children.indexOf(lastRelatedNode);
                    Element sibling = null;
                    for(int i = indexOfMe+1; i < lastRelatedNode.parent.children.size(); ++i){
                        sibling = lastRelatedNode.parent.children.get(i);
                        if(sibling == codeNode){
                            break;
                        }
                        sibling.toString(sb);
                    }
                }
            }
            codeNode.toString(sb);
            lastRelatedNode = codeNode;
        }
    }

    public List<Node> getRelations() {
        return relations;
    }

    public CodeType getCodeType() {
        return codeType;
    }
}
