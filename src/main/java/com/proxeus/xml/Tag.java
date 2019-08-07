package com.proxeus.xml;

import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Tag object keeps code, XML or text content.
 * The StringBuffer is usually the main reference for XML or for text content.
 * For code it is always a new buffer containing the cleaned code.
 */
public class Tag {
    protected TagType type;
    protected int startIndex;
    protected int endIndex;
    protected CodeType codeType = CodeType.NoCode;
    protected String name;
    /**
     * To not copy unneccessary string we keep the buffer here.
     * But we copy a string buffer for code though as it has to be cleaned.
     */
    protected StringBuffer content;
    private static final Pattern nameCodeReg = Pattern.compile("\\{%\\s*([^\\s\\/]+).*%\\}");
    private static final Pattern nameXmlReg = Pattern.compile("\\<\\/?([^\\s\\/]+).*\\/?\\>");
    private static final Pattern doubleQuotesEncodedRegex = Pattern.compile("(&quot;|“|”|„)");
    private static final Pattern singleQuotesEncodedRegex = Pattern.compile("(&apos;|’|‘|‚)");

    public Tag() {
    }
    public Tag(String contnt, TagType type) {
        this.codeType = CodeType.NoCode;
        this.content = new StringBuffer(contnt);
        this.startIndex = 0;
        this.endIndex = contnt.length();
        this.type = type;
        init();
    }
    public Tag(StringBuffer sb, int startIndex, int endIndex, TagType type) {
        this(CodeType.NoCode, sb, startIndex, endIndex, type);
    }

    public Tag(CodeType codeType, StringBuffer sb, int startIndex, int endIndex, TagType type) {
        this.codeType = codeType;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.content = sb;//sb.substring(startIndex, endIndex);
        this.type = type;
        init();
    }

    public Tag(CodeType codeType, List<Tag> styleTagsInsideCode, StringBuffer sb, int startIndex, int endIndex, TagType type) {
        this.codeType = codeType;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.type = type;
        cleanCode(sb, styleTagsInsideCode);
        init();
    }

    private void init(){
        if (codeType.isCode()) {
            if(codeType ==CodeType.CodeBlock){
                Matcher m = nameCodeReg.matcher(toCharSequence());
                if (m.find()) {
                    this.name = m.group(1).toLowerCase();
                    if (this.name.startsWith("end")) {
                        this.name = this.name.substring(3);
                        type = TagType.END;
                    }
                } else {
                    this.name = "";
                }
            }else{
                this.name = "";
            }
        } else {
            Matcher m = nameXmlReg.matcher(toCharSequence());
            this.name = m.find() ? m.group(1) : "";
        }
    }

    public String toString() {
        return this.content.substring(startIndex, endIndex);
    }

    public CharSequence toCharSequence(){
        return new NoCopyCharSequence(content, startIndex, endIndex);
    }

    public void toString(StringBuilder sb) {
        sb.append(this.content.substring(startIndex, endIndex));
    }

    public void toStream(OutputStream sb, Charset charset) throws IOException {
        sb.write(this.content.substring(startIndex, endIndex).getBytes(charset));
    }

    protected Tag copy() {
        Tag t = new Tag();
        t.type = this.type;
        t.codeType = this.codeType;
        t.name = this.name;
        t.content = this.content;
        t.startIndex = this.startIndex;
        t.endIndex = this.endIndex;
        return t;
    }

    protected Tag createEndTag() {
        Tag t = new Tag();
        t.type = this.type;
        t.codeType = this.codeType;
        t.name = this.name;
        String cont = "</" + this.name + ">";
        t.content = new StringBuffer(cont);
        t.startIndex= 0;
        t.endIndex = cont.length();
        return t;
    }

    protected Tag createStartTag() {
        Tag t = new Tag();
        t.type = this.type;
        t.codeType = this.codeType;
        t.name = this.name;
        String cont = "<" + this.name + ">";
        t.content = new StringBuffer(cont);
        t.startIndex= 0;
        t.endIndex = cont.length();
        return t;
    }

    protected boolean isCode(){
        return codeType.isCode();
    }

    protected boolean isNoCode(){
        return codeType.isNoCode();
    }

    /**
     * cleanCode cleans away all the tags inside code, decodes encoded characters, and replaces special characters.
     * Example:
     *  <b>{</b><b>{</b><b>...</b><b>}</b><b>}</b>
     * Outcome:
     *  <b>{{...}}</b>
     * @param styleTagsInsideCode the collected style tags
     * @return cleaned code
     */
    private void cleanCode(StringBuffer buf, List<Tag> styleTagsInsideCode){
        int startIndex = this.startIndex;
        if(styleTagsInsideCode.size()>0){
            StringBuffer sb = new StringBuffer();
            for (Tag t : styleTagsInsideCode) {
                sb.append(buf.substring(startIndex, t.startIndex));
                startIndex = t.endIndex;
            }
            sb.append(buf.substring(startIndex, endIndex));
            if(type != TagType.END){
                cleanCodeContent(sb);
            }else{
                this.content = new StringBuffer(sb.toString());
                this.startIndex = 0;
                this.endIndex = content.length();
            }
            return;
        }
        if(type != TagType.END){
            cleanCodeContent(buf.subSequence(startIndex, endIndex));
        }else{
            this.content = new StringBuffer(buf.subSequence(startIndex, endIndex));
            this.startIndex = 0;
            this.endIndex = content.length();
        }

    }

    private void cleanCodeContent(CharSequence cnt) {
        String content = cnt.toString();
        content = content.replace("&lt;", "<").replace("&gt;", ">");
        content = content.replace("–", "-").replace("&amp;", "&");
        content = content.replace("?.", ".");
        content = singleQuotesEncodedRegex.matcher(doubleQuotesEncodedRegex.matcher(content).replaceAll("\"")).replaceAll("'");
        this.content = new StringBuffer(content);
        this.startIndex = 0;
        this.endIndex = content.length();
    }

    /**
     * write attr value
     */
    public void attr(String attrName, String value) {
        value = StringEscapeUtils.escapeXml10(value);
        AttrParse ap = parseAttr(0);
        while(ap!=null){
            if(ap.content.substring(ap.coords[0], ap.coords[1]).equals(attrName)){
                //found the attr
                ap.content = ap.content.replace(ap.content.substring(ap.coords[2], ap.coords[3]), value);
                this.content = new StringBuffer(ap.content);
                startIndex = 0;
                endIndex = ap.content.length();
                return;
            }
            if(ap.coords[3] != 0){
                ap = parseAttr(ap.coords[3]);
            }else{
                ap = parseAttr(ap.coords[1]);
            }
        }
        String content = toString();
        content = content.replace(name, name+" "+attrName+"=\""+value+"\"");
        this.content = new StringBuffer(content);
        startIndex = 0;
        endIndex = content.length();
    }

    /**
     * read attr value
     */
    public String attr(String name){
        AttrParse ap = parseAttr(0);
        while(ap!=null){
            if(ap.content.substring(ap.coords[0], ap.coords[1]).equals(name)){
                //found the attr
                return StringEscapeUtils.unescapeXml(ap.content.substring(ap.coords[2], ap.coords[3]));
            }
            if(ap.coords[3] != 0){
                ap = parseAttr(ap.coords[3]);
            }else{
                ap = parseAttr(ap.coords[1]);
            }
        }
        return null;
    }

    private class AttrParse{
        String content;
        int[] coords;
    }
    /**
     * parser for reading and writing XML attributes
     * @param startAt this position
     * @return the coords of the attr name and value
     */
    private AttrParse parseAttr(int startAt){
        AttrParse attrParse = new AttrParse();
        attrParse.coords = new int[4];
        char currentChar = 0;
        char nextChar = 0;
        boolean insideAttrVal = false;
        boolean insideAttrName = false;
        char attrValWrapperChar = 0;
        CharSequence content;
        if((attrParse.content = toString()) != null){
            int size = attrParse.content.length();
            if(startAt<=0){
                startAt = name.length();
            }
            for(int i = startAt; i < size; ++i){
                currentChar = attrParse.content.charAt(i);
                if (i + 1 < size) {
                    nextChar = attrParse.content.charAt(i + 1);
                } else {
                    nextChar = 0;
                }
                if(!insideAttrName && !insideAttrVal && nextChar > 0 && Character.isSpaceChar(currentChar) && Character.isLetter(nextChar)){
                    insideAttrName = true;
                    attrParse.coords[0] = i + 1;
                }else if(insideAttrName && currentChar == '='){
                    insideAttrName = false;
                    attrParse.coords[1] = i;
                    if(nextChar > 0 && !Character.isSpaceChar(nextChar)){
                        insideAttrVal = true;
                    }
                } else if(insideAttrVal && attrParse.coords[2] == 0){
                    if(currentChar == '\'' || currentChar == '"'){
                        attrValWrapperChar = currentChar;
                        attrParse.coords[2] = i + 1;
                    }else{
                        attrValWrapperChar = 0;
                        attrParse.coords[2] = i;
                    }
                } else if (insideAttrVal && attrParse.coords[2] != 0 && (attrValWrapperChar == 0 && Character.isSpaceChar(currentChar) || attrValWrapperChar == currentChar)) {
                    attrParse.coords[3] = i;
                    return attrParse;
                }
            }
        }
        return null;
    }

    protected boolean whiteSpacesOnly(){
        char c = 0;
        for(int index = startIndex; index <= endIndex; ++index){
            c = this.content.charAt(index);
            if(Character.isWhitespace(c) || Character.isSpaceChar(c)){
                continue;
            }
            return false;
        }
        return true;
    }

    public int length(){
        return endIndex - startIndex;
    }
}
