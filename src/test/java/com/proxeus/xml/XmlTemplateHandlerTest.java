package com.proxeus.xml;

import com.proxeus.document.TemplateCompiler;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class XmlTemplateHandlerTest {
    //@Test
    public void parseODTXml() throws Exception{
        TemplateHandlerOld expected = getXmlTemplateFixer2("old/odt_content_fixed.xml");
        TemplateHandlerOld mxf = getXmlTemplateFixer2("old/odt_content.xml");
        String fixed_template_with_code = expected.toString();
        mxf.fixCodeStructures();
        String fixed = mxf.toString();
        System.out.println(fixed);
        Assert.assertEquals(fixed_template_with_code, fixed);

    }

    //@Test
    public void parseODTBodyXml() throws Exception{
        TemplateHandlerOld expected = getXmlTemplateFixer2("old/odt_body_fixed.xml");
        TemplateHandlerOld mxf = getXmlTemplateFixer2("old/odt_body.xml");
        String fixed_template_with_code = expected.toString();
        mxf.fixCodeStructures();
        String fixed = mxf.toString();
        System.out.println(fixed);
        Assert.assertEquals(fixed_template_with_code, fixed);

    }

    @Test
    public void parseIfStatementXml() throws Exception{
        TemplateHandlerOld expected = getXmlTemplateFixer2("old/if_statement_fixed.xml");
        TemplateHandlerOld mxf = getXmlTemplateFixer2("old/if_statement.xml");
        String fixed_template_with_code = expected.toString();
        mxf.fixCodeStructures();
        String fixed = mxf.toString();
        System.out.println(fixed);
        Assert.assertEquals(fixed_template_with_code, fixed);

    }

    @Test
    public void fixCodeStructures() throws Exception {
        TemplateHandlerOld expected = getXmlTemplateFixer2("old/template_with_code_fixed.xml");
        TemplateHandlerOld mxf = getXmlTemplateFixer2("old/template_with_code.xml");
        String fixed_template_with_code = expected.toString();
        List<Element> imgEles = mxf.findElementsByName("imgele");
        imgEles.get(0).attr("name:var", "img123");
        mxf.fixCodeStructures();
        String fixed = mxf.toString();
        Assert.assertEquals(fixed_template_with_code, fixed);
    }

    @Test
    public void fixCodeStructures2() throws Exception {
        TemplateHandlerOld expected = getXmlTemplateFixer2("old/template_with_code2_fixed.xml");
        TemplateHandlerOld mxf = getXmlTemplateFixer2("old/template_with_code2.xml");
        String fixed_template_with_code = expected.toString();
        mxf.fixCodeStructures();
        String fixed = mxf.toString();
        System.out.println(fixed);
        Assert.assertEquals(fixed_template_with_code, fixed);
    }

    @Test
    public void findElementsByNameFromRoot() throws Exception {
        TemplateHandlerOld mxf = getXmlTemplateFixer();
        List<Element> spans = mxf.findElementsByName("span");
        Assert.assertEquals(4, spans.size());
    }

    @Test
    public void findElementsByNameByChild() throws Exception {
        TemplateHandlerOld mxf = getXmlTemplateFixer();
        List<Element> lis = mxf.findElementsByName("li:a");
        List<Element> innerLi = lis.get(3).findElementByName("li:a");
        Assert.assertEquals(1, innerLi.size());
    }

    @Test
    public void attrWithValue() throws Exception {
        TemplateHandlerOld mxf = getXmlTemplateFixer();
        List<Element> lis = mxf.findElementsByName("li:a");
        List<Element> innerLi = lis.get(3).findElementByName("li:a");
        Element target = innerLi.get(0);
        target.attr("abc", "111");
        Assert.assertEquals("<li:a abc=\"111\">", target.start.toString());
        target.attr("hello", "123");
        Assert.assertEquals(true, target.start.toString().contains("hello=\"123\""));
    }

    @Test
    public void attrWithValue2() throws Exception {
        TemplateHandlerOld mxf = getXmlTemplateFixer();
        List<Element> lis = mxf.findElementsByName("li:a");
        List<Element> innerLi = lis.get(3).findElementByName("li:a");
        Element target = innerLi.get(0);
        Assert.assertEquals("4.1", target.attr("abc"));
        target.attr("hello", "123 \" ' ");
        Assert.assertEquals("123 \" ' ", target.attr("hello"));
    }

    @Test
    public void attrWithValue3() throws Exception {
        TemplateHandlerOld mxf = getXmlTemplateFixer();
        List<Element> lis = mxf.findElementsByName("draw:frame");
        List<Element> innerLi = lis.get(0).findElementByName("draw:image");
        Element target = innerLi.get(0);
        lis.get(0).attr("draw:name", "newDrawName...");
        target.attr("loext:mime-type", "image/png");
        Assert.assertEquals("image/png", target.attr("loext:mime-type"));
    }

    @Test
    public void rootNodeContainingCode() throws Exception {
        TemplateHandlerOld mxf = getXmlTemplateFixer();
        List<Element> lis = mxf.findElementsByName("draw:frame");
        List<Element> innerLi = lis.get(0).findElementByName("draw:image");
        Element target = innerLi.get(0);
        lis.get(0).attr("draw:name", "newDrawName...");
        target.attr("loext:mime-type", "image/png");
        mxf.fixCodeStructures();
        Node rootNodeContainingCode = mxf.getRootNodeContainingCode();
        Assert.assertEquals("body", rootNodeContainingCode.name());
    }

    private TemplateHandlerOld getXmlTemplateFixer() throws Exception {
        Config configTryToPlaceCodeMoreSuitable = new Config();
        configTryToPlaceCodeMoreSuitable.FixCodeByFindingTheNextCommonParent = true;
        configTryToPlaceCodeMoreSuitable.Fix_XMLTags = false;
        return new TemplateHandlerOld(configTryToPlaceCodeMoreSuitable, TemplateCompiler.class.getClassLoader().getResourceAsStream("old/parse_test.xml"));
    }

    private TemplateHandlerOld getXmlTemplateFixer2(String filename) throws Exception {
        Config configTryToPlaceCodeMoreSuitable = new Config();
        configTryToPlaceCodeMoreSuitable.FixCodeByFindingTheNextCommonParent = true;
        configTryToPlaceCodeMoreSuitable.AddTryToWrapXMLTagWithCode("for", "table:table-row");
        configTryToPlaceCodeMoreSuitable.Fix_XMLTags = true;
        configTryToPlaceCodeMoreSuitable.Fix_RemoveXMLStartTagsWithoutEndTags=true;
        return new TemplateHandlerOld(configTryToPlaceCodeMoreSuitable, TemplateCompiler.class.getClassLoader().getResourceAsStream(filename));
    }

}
