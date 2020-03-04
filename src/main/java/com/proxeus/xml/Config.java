package com.proxeus.xml;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Config for TemplateHandler
 */
public class Config {

    public Config(){
        AddCodeThatNeedsToMatchParentExactly("block", "macro"); //{% block %}, {% macro %}
    }
    /**
     * Example of empty XML wrappers if {%endfor%} is the target node.
     * ......
     * </table:table>
     * <text:p text:style-name="P93">//empty XML wrapper!
     * <text:span text:style-name="Source_20_Text"> //empty XML wrapper!
     * <text:span text:style-name="T189">{%endfor%}</text:span>//empty XML wrapper!
     * </text:span>
     * </text:p>
     * <table:table table:name="Table4" table:style-name="Table4">
     * .......
     *
     * null or 0 entries means no removal of empty wrappers
     */
    public Set<String> RemoveEmptyXMLWrappersAroundCode;
    public void AddTagNamesForRemovalAroundCode(String...n){
        if(n == null || n.length==0){
            return;
        }
        if (RemoveEmptyXMLWrappersAroundCode==null) {
            RemoveEmptyXMLWrappersAroundCode = new HashSet<>();
        }
        for(String xmlTagName : n){
            RemoveEmptyXMLWrappersAroundCode.add(xmlTagName);
        }
    }
    public boolean HasRemoveEmptyXMLWrappersAroundCodeEntries(){
        return RemoveEmptyXMLWrappersAroundCode != null && RemoveEmptyXMLWrappersAroundCode.size()>0;
    }

    public Set<String> MatchParentExactlyCodes = new HashSet<>();
    public void AddCodeThatNeedsToMatchParentExactly(String...n){
        if(n == null || n.length==0){
            return;
        }
        for(String codeName : n){
            MatchParentExactlyCodes.add(codeName);
        }
    }

    //false means tags are not going to be fixed with an end or start tag close to the wrong tag
    public boolean Fix_XMLTags;

    //precondition Fix_XMLTags = true
    //false means they will be fixed by providing a start tag close to the end tag
    public boolean Fix_RemoveXMLEndTagsWithoutStartTags;

    //precondition Fix_XMLTags = true
    //false means they will be fixed by providing an end tag close to the start tag
    public boolean Fix_RemoveXMLStartTagsWithoutEndTags;

    /**
     * Example:
     * <ul:abc abc="1">
     * {% set abc = "% } {{}}" %}
     * {% set abc2 = [] %}
     * <li:a abc="1">
     * {%if (true)%}
     * <span>before if{%if (false)%} after if</span>
     * </li:a>
     * <li:a abc="2">
     * <span>before elseif{%elseif (false) %} after elseif</span>
     * </li:a>
     * <li:a abc="3">
     * <span>before else{%else%} after else</span>
     * <div>{%if input.IntendedDeliveryDate == ‘Specific Date‘ %}<p>{{input.SpecificDate}}{%else%}</p>{{input.DeliveryWeek}}<p>{%endif%}</p></div>
     * </li:a>
     * <li:a abc="4">
     * <ul:abc abc="4.1">
     * <li:a abc="4.1">
     * <span>lala{%endif %} omfg</span>
     * </li:a>
     * </ul:abc>
     * {%endif (true) %}
     * </li:a>
     * </ul:abc>
     *
     *
     * Outcome:
     * <ul:abc abc="1">
     * {% set abc = "% } {{}}" %}
     * {% set abc2 = [] %}
     * {%if (true)%}
     * <li:a abc="1">
     * <span>before if</span>
     * </li:a>{%if (false)%}
     * <li:a abc="1">
     * <span> after if</span>
     * </li:a>
     * <li:a abc="2">
     * <span>before elseif</span>
     * </li:a>{%elseif false%}
     * <li:a abc="2">
     * <span> after elseif</span>
     * </li:a>
     * <li:a abc="3">
     * <span>before else</span>
     * </li:a>{%else%}
     * <li:a abc="3">
     * <span> after else</span>
     * {%if input.IntendedDeliveryDate == ‘Specific Date‘ %}{{input.SpecificDate}}{%else%}{{input.DeliveryWeek}}{%endif%}
     * </li:a>
     * <li:a abc="4">
     * <ul:abc abc="4.1">
     * <li:a abc="4.1">
     * <span>lala</span>
     * </li:a>
     * </ul:abc>
     * </li:a>{%endif (false) %}
     * <li:a abc="4">
     * <ul:abc abc="4.1">
     * <li:a abc="4.1">
     * <span> omfg</span>
     * </li:a>
     * </ul:abc>
     * </li:a>{%endif (true) %}
     * </ul:abc>
     */
    public boolean FixCodeByFindingTheNextCommonParent;

    /**
     * How many times should be tried to find the best suitable place for code configured in {@link #TryToWrapXMLTagWithCode}
     * 1-4 could be a good choice, higher definitions could lead to unexpected behaviours in huge XML's.
     */
    public short TrialCountForWrappingTagWithCode = 4;

    /**
     * type  1 2
     * String[][]
     * 1.) first list of conditions []
     * 2.) second Code on [0] and the XML to be wrapped on [1]
     *
     * like: [['for', 'table:row'], ['if','span']]
     *
     * To handle cases like:
     * <table abc="4">
     * <table:row abc="4.1">
     * {% for item in list %}
     * <table:cell abc="4.1">
     * <span>lala{%endif (false) %} omfg</span>
     * {% endfor %}
     * </table:cell>
     * </table:row>
     * </table>
     * <p>
     *
     * outcome:
     * <table abc="4">
     * {% for item in list %}
     * <table:row abc="4.1">
     * <table:cell abc="4.1">
     * <span>lala{%endif (false) %} omfg</span>
     * </table:cell>
     * </table:row>
     * {% endfor %}
     * </table>
     */
    public Map<String,Set<String>> TryToWrapXMLTagWithCode;
    public void AddTryToWrapXMLTagWithCode(String codeName, String...xmlTagName){
        if(xmlTagName == null || xmlTagName.length==0){
            return;
        }
        if (TryToWrapXMLTagWithCode==null) {
            TryToWrapXMLTagWithCode = new HashMap<>();
        }
        Set<String> xmlTagNames = TryToWrapXMLTagWithCode.get(codeName);
        if(xmlTagNames == null){
            xmlTagNames = new HashSet<>(xmlTagName.length);
            TryToWrapXMLTagWithCode.put(codeName, xmlTagNames);
        }
        for(String xmlTag : xmlTagName){
            xmlTagNames.add(xmlTag);
        }
    }

    public boolean HasTryToWrapXMLTagWithCodeFor(String codeName){
        return TryToWrapXMLTagWithCode != null && TryToWrapXMLTagWithCode.size()>0 && TryToWrapXMLTagWithCode.containsKey(codeName);
    }
}
