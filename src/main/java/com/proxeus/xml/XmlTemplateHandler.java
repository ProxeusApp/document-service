package com.proxeus.xml;

import com.proxeus.xml.reader.InputStreamReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * XmlTemplateHandler is the entry point and defines the core.
 *
 * The InputStreamReader of this handler is charset extendable after initialization.
 * While the XML is being read, this handler extends reader as soon as the charset as been found.
 *
 * Here a very vague description of the responsibility:
 * 1. reading and structuring content to code
 * 2. cleaning style/formatting tags out of the code
 * 3. placing code meaningful (can be enabled/disabled via config)
 * 4. fixing broken XML nodes (can be enabled/disabled via config)
 *
 * For more information please checkout the child classes in this packages such as Code, Node, Element or Tag.
 */
public class XmlTemplateHandler {
    private final static int BUFFER_SIZE = 8192;
    private InputStreamReader reader;
    private StringBuffer content;
    private boolean reachedEOF;
    private int currentIndex;
    private List<Node> codesToRelate = new ArrayList<>(80);
    private List<Tag> styleTagsInsideCode = new ArrayList<>(10);
    private List<Code> codesRelated = new ArrayList<>(120);
    private Config config;
    private Node targetNode;
    private char[] code = new char[3];
    private int[] codePos = new int[3];
    private Node rootNode;
    private Charset charset = null;
    //enables code escape feature
    private final static String verbatim = "verbatim";

    /**
     * helper for finding the root node containing code
     * to prevent from compiling unnecessary huge XML without code
     */
    private Node firstCodeNode = null;
    private Node lastCodeNode = null;

    public XmlTemplateHandler(Config config, InputStream inStream) throws Exception {
        this(config, inStream, BUFFER_SIZE*10);
    }

    public XmlTemplateHandler(Config config, InputStream inStream, long fileSize) throws Exception {
        this.config = config;
        this.content = new StringBuffer(fileSize<=0?BUFFER_SIZE*10:(int)fileSize);
        this.reader = new InputStreamReader(inStream, BUFFER_SIZE);
        rootNode = parse();
    }

    public boolean containsCode(){
        return rootNode != null && codesRelated.size()>0;
    }

    /**
     * Fixes code structures that are cutting across.
     * Fixes code relations that have different parents.
     */
    public void fixCodeStructures() throws Exception {
        if (rootNode != null) {
            boolean removeEmptyXMLWrappersAroundCode = config.HasRemoveEmptyXMLWrappersAroundCodeEntries();
            Code firstMacroCode = null;
            if (removeEmptyXMLWrappersAroundCode || config.FixCodeByFindingTheNextCommonParent) {
                Iterator<Code> codeIterator = codesRelated.iterator();
                Code code = null;
                while (codeIterator.hasNext()) {
                    code = codeIterator.next();
                    if(firstMacroCode == null && code.isMacroBlock()){
                        firstMacroCode = code;
                    }
                    fixCode(removeEmptyXMLWrappersAroundCode, code);
                }
            }
            if(firstMacroCode != null){
                //to support simple macro usage by {{macros.function...}}
                //and to recognize it via *macros.* so we can remove the empty wrappers safely on {{..}} too
                //the removal is import to support inline paragraph templates
                firstMacroCode.doMacroImport();
            }
            if (config.Fix_XMLTags) {
                fixXMLElements(rootNode);
            }
        }
    }

    /**
     * Parse the input.
     * @return root node
     */
    private Node parse() throws Exception {
        currentIndex = 0;
        readMore();//initial read to fill the buffer
        List<Node> nodes = new ArrayList<>(200);
        Tag tag = null;
        Node rootNode = null;
        boolean ignoreCode = false;
        if((tag = nextTag(ignoreCode, false)) != null){
            targetNode = rootNode = new Node(tag);
            //expected <?xml version="1.0" encoding="UTF-8"?>
            //if such a header is not provided, there could be encoding issues
            if(tag.type == TagType.HEADER){
                String encoding = rootNode.attr("encoding");
                if(encoding != null){
                    encoding = encoding.trim();
                    if(Charset.isSupported(encoding)){
                        charset = Charset.forName(encoding);
                        reader.initCharset(charset);
                    }
                }
                //to prevent from any node specific actions on the header
                rootNode.type = ElementType.TEXT;
            }
        }
        if(rootNode == null){
            throw new Exception("could'n parse a root node");
        }
        Node childNode;

        while ((tag = nextTag(ignoreCode, false)) != null) {
            if (tag.type == TagType.START_AND_END) {
                childNode = new Node(tag);
                targetNode.addChild(childNode);
                if (tag.isCode()) {
                    childNode.interestingForLoopBack = false;
                    //adding it directly to the related ones as we are not going to look for a related block
                    codesRelated.add(new Code(tag.codeType, childNode));
                    mightBeFirst(childNode);
                    lastCodeNode = childNode;
                }
            } else if (tag.type == TagType.START) {
                childNode = new Node(tag);
                targetNode.addChild(childNode);
                if (tag.isCode()) {
                    if(tag.name.equals(verbatim)){
                        ignoreCode = true;
                    }
                    //might need to look after this node if we get the corresponding end tag
                    codesToRelate.add(childNode);
                    mightBeFirst(childNode);
                } else {
                    //a xml start tag, add it to the loop back as we need to find the corresponding end tag and make it the new target node
                    //as we need to add the upcoming children to the correct parent
                    nodes.add(childNode);
                    targetNode = childNode;
                }
            } else if (tag.type == TagType.END) {
                Node loopNode = null;
                if (tag.isCode()) {
                    //handle code relations independent from xml nodes as they are probably cutting across
                    boolean handled = false;
                    Node endCode = new Node(null);
                    endCode.setEnd(tag);
                    targetNode.addChild(endCode);
                    boolean isIf = tag.name.equals("if");
                    Code targetCode = new Code(tag.codeType, endCode);
                    //loop back to find the corresponding start node
                    //for special cases like if-else, we need to find n related nodes
                    for (int i = codesToRelate.size() - 1; i > -1; --i) {
                        loopNode = codesToRelate.get(i);
                        if (loopNode.interestingForLoopBack && loopNode.hasNoEndTag()) {
                            if (isIf) {
                                if (loopNode.name().matches("if|elseif|else")) {
                                    loopNode.interestingForLoopBack = false;
                                    //remove from the code loop back list as we won't need it anymore
                                    //codesToRelate.remove(loopNode);
                                    //prepend the next related node before endif, else, elseif or if
                                    targetCode.prepend(loopNode);
                                    if ("if".equals(loopNode.name())) {
                                        //break prepending with the if as this is the last one belonging to this very family
                                        handled = true;
                                        break;
                                    }
                                }
                            } else if (tag.name.equals(loopNode.name())) {
                                loopNode.interestingForLoopBack = false;
                                //remove from the code loop back list as we won't need it anymore
                                //codesToRelate.remove(loopNode);
                                //prepend the next related node before the corresponding end node for any code block pair like {% for %}..{% endfor %}
                                targetCode.prepend(loopNode);
                                handled = true;
                                break;
                            }
                        }
                    }
                    lastCodeNode = endCode;
                    if(handled){
                        if(tag.name.equals(verbatim)){
                            ignoreCode = false;
                        }
                        //found the relations for this targetCode, add it to the codesRelated list for fixing patterns that are cutting across and so on
                        codesRelated.add(targetCode);
                    }else{
                        //this will cause an exception during compilation, trying to fix this will lead to unexpected behaviours
                        //just add it to the unrelated code list although we are not going to do anything with it
                        //might need it in a later version for anything...
                        endCode.interestingForLoopBack = false;
                        codesToRelate.add(endCode);
                    }
                } else {
                    int foundIndex = -1;
                    for (int i = nodes.size() - 1; i > -1; --i) {
                        loopNode = nodes.get(i);
                        if (loopNode.interestingForLoopBack && loopNode.hasNoEndTag() && tag.name.equals(loopNode.name())) {
                            //fount the start node containing the start tag that belongs to this end tag
                            loopNode.setEnd(tag);
                            //ensure this tag is not going to be the parent of any element anymore
                            loopNode.interestingForLoopBack = false;
                            //as this node is closed now, we need to switch the targetNode node to the parent node that is still looking for an end tag
                            setTargetNodeToOuterWithNoEndTagNode(loopNode);
                            foundIndex = i;
                            break;
                        }
                    }
                    if (foundIndex > -1) {
                        // now set all nodes without an end tag to failed and remove them from the loop back, starting at the index where the start tag was found
                        // as we won't need them anymore for loop back's
                        ListIterator<Node> nodeIterator = nodes.listIterator(foundIndex);
                        while (nodeIterator.hasNext()) {
                            loopNode = nodeIterator.next();
                            if (loopNode.hasNoEndTag()) {
                                //ensure this tag is not going to be the parent of any element anymore
                                loopNode.interestingForLoopBack = false;
                            }
                            nodeIterator.remove();
                        }
                    } else {
                        Node endTagWithoutStartTag = new Node(null);
                        endTagWithoutStartTag.setEnd(tag);
                        endTagWithoutStartTag.interestingForLoopBack = false;
                        targetNode.addChild(endTagWithoutStartTag);
                    }
                }
            }
        }

        ListIterator<Node> nodeIterator = codesToRelate.listIterator();
        while (nodeIterator.hasNext()) {
            childNode = nodeIterator.next();
            if (childNode.interestingForLoopBack && childNode.hasNoEndTag() && childNode.start.type == TagType.START) {
                childNode.interestingForLoopBack = false;
                //assume it is code like {% set ... %} without ending block
                //if it is another kind of code, it will throw errors during compilation anyway
                childNode.start.type = TagType.START_AND_END;
                childNode.moveChildsToParentAtTheSamePosition();
                codesRelated.add(new Code(childNode.start.codeType, childNode));
                nodeIterator.remove();
            }
        }
        codesToRelate.clear();
        if(lastCodeNode == null && firstCodeNode != null){
             lastCodeNode = firstCodeNode;
        }
        return rootNode;
    }

    private void mightBeFirst(Node n){
        if(firstCodeNode == null){
            firstCodeNode = n;
        }
    }

    /**
     * Find the next correct parent that will contain the upcoming children while reading.
     */
    private void setTargetNodeToOuterWithNoEndTagNode(Node maybeTarget) {
        targetNode = maybeTarget;
        while (targetNode.parent != null) {
            if (targetNode.type != ElementType.CODE && targetNode.interestingForLoopBack && targetNode.hasNoEndTag()) {
                return;
            }
            targetNode = targetNode.parent;
        }
    }

    /**
     * Loop recursively through the DOM and check/fix broken XML tags.
     * @param root element
     */
    private void fixXMLElements(Element root) {
        ListIterator<Element> iterator = root.children.listIterator();
        while (iterator.hasNext()) {
            Element ele = iterator.next();
            if (ele.type == ElementType.XML) {
                fixXMLElement(ele);
                if (ele.hasChildren()) {
                    fixXMLElements(ele);
                }
            }
        }
    }

    /**
     * Fix the provided XML element.
     * @param ele the might need to be fixed
     */
    private void fixXMLElement(Element ele) {
        if (ele.hasNoEndTag()) {
            if (config.Fix_RemoveXMLStartTagsWithoutEndTags) {
                ele.removeThisElementButKeepTheChildren();
            } else {
                ele.createEndTag();
            }
        } else if (ele.hasNoStartTag()) {
            if (config.Fix_RemoveXMLEndTagsWithoutStartTags) {
                ele.removeThisElementButKeepTheChildren();
            } else {
                ele.createStartTag();
            }
        }
    }

    /**
     * Fix code that would break the XML structure after compilation, if we would leave it as is.
     * @param removeEmptyXMLWrappersAroundCode whether to do further checks for removal or not
     * @param code that we fix if neccessary
     */
    private void fixCode(boolean removeEmptyXMLWrappersAroundCode, Code code) {
        if (removeEmptyXMLWrappersAroundCode && code.shouldRemoveEmptyXMLWrappersAroundThisCode()) {
            for(Node codeNode : code.relations){
                codeNode.removeEmptyXMLWrappers(config.RemoveEmptyXMLWrappersAroundCode);
            }
        }

        //try complicated fixes only on code blocks or blocks who doesn't have the same parent
        //code with one relation are either bad written blocks or single blocks
        if(code.isComment || code.relations.size()==1 || code.haveTheSameParent()){
            return;
        }
        //try to wrap feature is only supported for usual blocks with a staring and an ending code
        //assuming size == 2 is {%start%}...{%endstart%}
        if(code.relations.size()==2 && config.HasTryToWrapXMLTagWithCodeFor(code.relations.get(0).name())){
            code.tryToWrapXMLTag(
                    config.TryToWrapXMLTagWithCode.get(code.relations.get(0).name()),
                    config.TrialCountForWrappingTagWithCode);
        }
        if (config.FixCodeByFindingTheNextCommonParent) {
            code.moveToCommonParent(config);
        }
    }

    /**
     * nextTag provides the next tag from the current position.
     *
     * Text content is handled internally if breakOnText is false.
     * @param ignoreCode whether to start ignoring or not. Code is only being ignored if wrapped with {% verbatim %} .. ignored code .. {% endverbatim %}
     * @param breakOnText false means it adds the text content silently to the targetNode, true means it breaks the lookup if it reaches a text element with null
     * @return the parsed tag. The tag can be <any>, </any>, <any/>, {{any..}}, {% any ..%} or {# any ..#} .
     */
    private Tag nextTag(boolean ignoreCode, boolean breakOnText) throws Exception {
        int startIndex = -1;
        int slashIndex = 0;
        int exclamationMarkIndex = -1;
        int questionMark1Index = -1;
        int questionMark2Index = -1;
        int equalitySign = -1;
        char openAttrQuote = 0;
        boolean insideAttrVal = false;
        ElementType currentType = ElementType.TEXT;
        Tag t = null;
        for (;hasMore(currentIndex); ++currentIndex) {
            char cc = content.charAt(currentIndex);
            char nc;
            if (hasMore(currentIndex + 1)) {
                nc = content.charAt(currentIndex + 1);
            } else {
                nc = 0;
            }
            if (currentType == ElementType.CODE) {
                //parse code
                if (cc == '}' || cc == '%' || cc == '#') {
                    if (code[2] == '%' && cc == '}' && code[0] == '{' && code[1] == '%' && codePos[2] + 1 == indexWithoutStyleTagsBetweenCodeChars(codePos[2], styleTagsInsideCode)) {
                        ++currentIndex;
                        t = new Tag(CodeType.CodeBlock, styleTagsInsideCode, content, startIndex, currentIndex, TagType.START);
                    } else if (code[2] == '}' && cc == '}' && code[0] == '{' && code[1] == '{' && codePos[2] + 1 == indexWithoutStyleTagsBetweenCodeChars(codePos[2], styleTagsInsideCode)) {
                        ++currentIndex;
                        t = new Tag(CodeType.Output, styleTagsInsideCode, content, startIndex, currentIndex, TagType.START_AND_END);
                    } else if (code[2] == '#' && cc == '}' && code[0] == '{' && code[1] == '#' && codePos[2] + 1 == indexWithoutStyleTagsBetweenCodeChars(codePos[2], styleTagsInsideCode)) {
                        ++currentIndex;
                        t = new Tag(CodeType.Comment, styleTagsInsideCode, content, startIndex, currentIndex, TagType.START_AND_END);
                    } else {
                        code[2] = cc;
                        codePos[2] = currentIndex;
                    }
                    if (t != null && t.isCode()) {
                        if(ignoreCode && (t.codeType != CodeType.CodeBlock || !t.name.equals(verbatim))){
                            //escape code inside {% verbatim %} code {% endverbatim %}
                            styleTagsInsideCode.clear();
                            currentIndex = startIndex = codePos[0];
                            currentType = ElementType.TEXT;
                            continue;
                        }
                        if(styleTagsInsideCode.size()>0){
                            Tag firstNoneSpaceTag = styleTagsInsideCode.get(0);
                            Tag lastStyleTagInsideCode = styleTagsInsideCode.get(styleTagsInsideCode.size()-1);
                            if (firstNoneSpaceTag != null && firstNoneSpaceTag.type == TagType.END
                                    || (lastStyleTagInsideCode != null && lastStyleTagInsideCode.type == TagType.START)) {
                                //must have close
                                int currInd = currentIndex;
                                Tag endTagOutSideCode = nextTag(ignoreCode, true);
                                if (endTagOutSideCode == null || endTagOutSideCode.isCode() || (endTagOutSideCode.type == TagType.END && targetNode.name().equals(endTagOutSideCode.name))) {
                                    //reset index
                                    currentIndex = currInd;
                                } else {
                                    //skip this tag and reorganize targetNode to ensure it is well formed
                                    if(targetNode.parent!= null){
                                        Node tn = targetNode;
                                        tn.moveChildsToParentAtTheSamePosition();
                                        setTargetNodeToOuterWithNoEndTagNode(tn.parent);
                                        tn.remove();
                                    }
                                }
                            }
                            //gc help
                            styleTagsInsideCode.clear();
                        }
                        return t;
                    }
                }else if (cc == '<' && (nc == '/' || Character.isLetter(nc))) {
                    //if start or end tag, it has to be a style tag inside code
                    //collect it separately
                    Tag dirtyStyleTag = nextTag(ignoreCode, false);
                    if (dirtyStyleTag != null) {
                        styleTagsInsideCode.add(dirtyStyleTag);
                        --currentIndex;
                    }
                }
            } else if (currentType == ElementType.XML) {
                if (cc == '/') {
                    slashIndex = currentIndex;
                } else if (cc == '=') {
                    if (equalitySign == -1) {
                        equalitySign = currentIndex;
                    }
                } else if (cc != '"' && cc != '\'') {
                    if (cc == '>' && !insideAttrVal) {
                        t = null;
                        if (questionMark1Index - 1 != startIndex && questionMark2Index + 1 != currentIndex && exclamationMarkIndex - 1 != startIndex) {
                            if (slashIndex + 1 != currentIndex && slashIndex - 1 != startIndex) {
                                ++currentIndex;
                                t = new Tag(content, startIndex, currentIndex, TagType.START);
                            } else if (slashIndex - 1 == startIndex) {
                                ++currentIndex;
                                t = new Tag(content, startIndex, currentIndex, TagType.END);
                            } else if (slashIndex + 1 == currentIndex) {
                                ++currentIndex;
                                t = new Tag(content, startIndex, currentIndex, TagType.START_AND_END);
                            }
                        } else {
                            ++currentIndex;
                            t = new Tag(content, startIndex, currentIndex, TagType.HEADER);
                        }
                        return t;
                    }
                    if (cc == '?' && (startIndex == currentIndex - 1 || nc == '>')) {
                        if (questionMark1Index == -1) {
                            questionMark1Index = currentIndex;
                        } else if (questionMark2Index == -1) {
                            questionMark2Index = currentIndex;
                        }
                    } else if (cc == '!' && startIndex == currentIndex - 1) {
                        exclamationMarkIndex = currentIndex;
                    }
                } else if (equalitySign + 1 == currentIndex) {
                    openAttrQuote = cc;
                    insideAttrVal = true;
                } else if (cc == openAttrQuote && (nc == '>' || nc == '/' || Character.isWhitespace(nc) || Character.isSpaceChar(nc))) {
                    openAttrQuote = 0;
                    insideAttrVal = false;
                }
            } else if (cc == '<') {
                if (startIndex != -1) {
                    if(breakOnText){
                        return null;
                    }
                    createTextElement(startIndex, currentIndex);
                }
                startIndex = currentIndex;
                slashIndex = 0;
                exclamationMarkIndex = -1;
                questionMark1Index = -1;
                questionMark2Index = -1;
                equalitySign = -1;
                openAttrQuote = 0;
                insideAttrVal = false;
                currentType = ElementType.XML;
                //for code
            } else if (cc == '{'){
                int currIndexForReset = currentIndex;
                CodeResult cr = nextCharWithoutStyleTags(ignoreCode, true);
                //make sure {%, {# and {{ are next to each other by skipping style tags between the first and the second char that look like "{<dirtystyletag>{"
                if((cr.nextChar == '{' || cr.nextChar == '%' || cr.nextChar == '#') && currIndexForReset + 1 == indexWithoutStyleTagsBetweenCodeChars(currIndexForReset, cr.styleTagsInsideCode)){
                    if (startIndex != -1) {
                        if(breakOnText){
                            return null;
                        }
                        createTextElement(startIndex, currIndexForReset);
                    }
                    if(cr.styleTagsInsideCode!= null){
                        styleTagsInsideCode = cr.styleTagsInsideCode;
                        cr.styleTagsInsideCode = null;
                    }else{
                        styleTagsInsideCode.clear();
                    }
                    startIndex = currIndexForReset;
                    slashIndex = 0;
                    exclamationMarkIndex = -1;
                    questionMark1Index = -1;
                    questionMark2Index = -1;
                    equalitySign = -1;
                    openAttrQuote = 0;
                    insideAttrVal = false;
                    currentType = ElementType.CODE;

                    code[0] = cc;
                    code[1] = cr.nextChar;
                    code[2] = 0;
                    codePos[0] = currIndexForReset;
                    codePos[1] = cr.nextCharIndex;
                    codePos[2] = -1;
                    continue;
                }
                //reset because of nextCharWithoutStyleTags
                currentIndex = currIndexForReset;
                if(startIndex == -1){
                    startIndex = currentIndex;
                    currentType = ElementType.TEXT;
                }
            } else if (startIndex == -1) {
                startIndex = currentIndex;
                currentType = ElementType.TEXT;
            }
        }

        return null;
    }

    /**
     * This method prevents from compiling unnecessary parts of the XML which are very heavy but do not contain any code.
     *
     * Retrieves the root node containing code pieces, like:
     * <a>
     *    <b> <----- root node containing code
     *        <c>
     *            {{..}}
     *        </c>
     *        <c>
     *            {%if%}..{%endif%}
     *            {{..}}
     *        </c>
     *    </b>
     * </a>
     * @return the root node containing code
     */
    public Node getRootNodeContainingCode(){
        if(firstCodeNode == null){
            return null;
        }
        return new Code(CodeType.CodeBlock, firstCodeNode, lastCodeNode).findTheCommonParent();
    }

    /**
     * Find code element by name
     * @return list of the findings
     */
    public List<Element> findCodeElementsByName(String nameRegex) {
        List<Element> codeElements = new ArrayList<>();
        for(Code code : codesRelated){
            for(Node relatedCode : code.relations){
                if(relatedCode.name().matches(nameRegex)){
                    codeElements.add(relatedCode);
                }
            }
        }
        return codeElements;
    }

    /**
     * Find code by name.
     * @return the list of the findings
     */
    public List<Code> findCodeByName(String nameRegex) {
        List<Code> codeElements = new ArrayList<>();
        for(Code code : codesRelated){
            for(Node relatedCode : code.relations){
                if(relatedCode.name().matches(nameRegex)){
                    codeElements.add(code);
                    break;
                }
            }
        }
        return codeElements;
    }

    /**
     * Find vars with prefix.
     * @param varPrefix can be null or ""
     * @return the variable set
     */
    public Set<String> findVars(String varPrefix) throws Exception {
        VarParser varParser = new VarParser(varPrefix);
        findVars(varParser);
        return varParser.Vars();
    }

    /**
     * Find vars.
     * @param varParser will be filled with the vars
     */
    public void findVars(VarParser varParser) throws Exception {
        for(Code code : codesRelated){
            if(code.isComment){
                continue;
            }
            for(Node relatedCode : code.relations){
                if(!relatedCode.isEndTagOnly()){
                    varParser.Parse(relatedCode.toString());
                }
            }
        }
    }

    public List<Code> codes(){
        return codesRelated;
    }

    /**
     * Find elements by name.
     */
    public List<Element> findElementsByName(String name) throws Exception {
        return rootNode.findElementByName(name);
    }

    /**
     * Look ahead helper class
     */
    private class CodeResult {
        char nextChar;
        int nextCharIndex;
        List<Tag> styleTagsInsideCode;
    }

    /**
     * Look ahead for the next char without tags. Tags are being collected in #styleTagsInsideCode.
     * @param ignoreCode true if we are inside a verbatim block
     * @param breakOnText true if we should break on text element
     * @return the findings
     */
    private CodeResult nextCharWithoutStyleTags(boolean ignoreCode, boolean breakOnText) throws Exception {
        CodeResult cr = new CodeResult();
        for (++currentIndex;hasMore(currentIndex); ++currentIndex) {
            cr.nextChar = content.charAt(currentIndex);
            cr.nextCharIndex = currentIndex;
            char nc;
            if (hasMore(currentIndex + 1)) {
                nc = content.charAt(currentIndex + 1);
            } else {
                nc = 0;
            }
            //if start or end tag, it has to be a style tag inside code
            if (cr.nextChar == '<' && (nc == '/' || Character.isLetter(nc))) {
                //collect it separately
                Tag dirtyStyleTag = nextTag(ignoreCode, breakOnText);
                if(dirtyStyleTag == null){
                    break;
                }
                if(cr.styleTagsInsideCode==null){
                    cr.styleTagsInsideCode = new ArrayList<>(2);
                }
                cr.styleTagsInsideCode.add(dirtyStyleTag);
                --currentIndex;
            }else{
                break;
            }
        }
        return cr;
    }

    /**
     * hasMore checks the content buffer if it is higher than the current index.
     * If we reached the end of the current buffer, it loads more.
     * If we reached the end of the file, it returns false.
     * @param currentIndex calc from this index
     * @return true if index is not at the end of the buffer or if we can load more. False if we reached the end.
     */
    private boolean hasMore(int currentIndex) throws Exception {
        if(currentIndex<content.length()){
            return true;
        }
        if(reachedEOF) {
            return false;
        }
        readMore();
        return hasMore(currentIndex);
    }

    private char[] readBuffer = new char[BUFFER_SIZE];
    /**
     * readMore fills the main buffer to keep the process flowing
     */
    public void readMore() throws Exception {
        int len = reader.read(readBuffer);
        if(len == -1){
            reachedEOF = true;
            return;
        }
        content.append(readBuffer, 0, len);
    }

    /**
     * Calculate the index by skipping style tags, so we can tell if for example the first '{' is followed by '%'.
     * @param startIndex start calculation from this index
     * @param styleTagsInsideCode the style tags that have been collected so far
     * @return the index without style tags
     */
    private int indexWithoutStyleTagsBetweenCodeChars(int startIndex, List<Tag> styleTagsInsideCode) {
        if(styleTagsInsideCode != null){
            int indexWithoutStyleTags = currentIndex;
            for (Tag t : styleTagsInsideCode) {
                if(t.startIndex>startIndex){
                    indexWithoutStyleTags -= t.endIndex - t.startIndex;
                }
            }
            return indexWithoutStyleTags;
        }
        return currentIndex;
    }

    /**
     * Creates a text element and adds it silently to the target node.
     */
    private void createTextElement(int startIndex, int currentIndex) {
        targetNode.addChild(new Element(new Tag(content, startIndex, currentIndex, TagType.TEXT)));
    }

    public int length(){
        if(rootNode!=null){
            return rootNode.length();
        }
        return 0;
    }

    public String toString(){
        if(rootNode != null){
            StringBuilder sb = new StringBuilder(length());
            rootNode.toString(sb);
            return sb.toString();
        }
        return "";
    }

    public void toString(StringBuilder sb){
        if(rootNode != null){
            rootNode.toString(sb);
        }
    }

    public InputStream toInputStream(){
        if(rootNode != null){
            StringBuilder sb = new StringBuilder(length());
            rootNode.toString(sb);
            return new ByteArrayInputStream(sb.toString().getBytes(charset));
        }
        return null;
    }

    public void toOutputStream(OutputStream outputStream) throws IOException {
        if(rootNode != null){
            rootNode.toOutputStream(outputStream, charset);
        }
    }

    public Charset getCharset(){
        return charset;
    }

    /**
     * help gc
     */
    public void free(){
        reader = null;
        content = null;
        codesToRelate = null;
        styleTagsInsideCode = null;
        codesRelated = null;
        config = null;
        targetNode = null;
        rootNode = null;
    }

}
