package com.proxeus.xml;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class VarParserTest {
    @Test
    public void parser() throws Exception {
        VarParser parser = new VarParser(null);
        List<String> codes = new ArrayList<>();
        codes.add("{{easy}}                                                           ".trim());
        codes.add("{% set easy=\"Proxeus makes document templating easier\" %}        ".trim());
        codes.add("{{input.ImageFile1}}                                               ".trim());
        codes.add("{{input.Var1*input.Var2}}                                          ".trim());
        codes.add("{{input.Var1*input.Var2}}                                          ".trim());
        codes.add("{{input.Var3/input.Var2}}                                          ".trim());
        codes.add("{{input.Var3/input.Var2}}                                          ".trim());
        codes.add("{{input.Var4}}                                                     ".trim());
        codes.add("{{(input.Var1>8)?\"Var is higher than 8\":\"Var is lower than 8\"}}".trim());
        codes.add("{{default(undefinedVariable,'World')}}                             ".trim());
        codes.add("{%if (condition)%}                                                 ".trim());
        codes.add("{%elseif (condition2)%}                                            ".trim());
        codes.add("{{loop.index0}}                                                    ".trim());
        codes.add("{{loop.index}}                                                     ".trim());
        codes.add("{{item}}                                                           ".trim());
        codes.add("{% for item in [1,2,3] %}                                          ".trim());
        codes.add("{{total*1000|number_format(2, \".\", \"'\")}}                      ".trim());
        codes.add("{{loop.index0}}                                                    ".trim());
        codes.add("{{loop.index}}                                                     ".trim());
        codes.add("{{item}}                                                           ".trim());
        codes.add("{% for item in [1,2,3] %}                                          ".trim());
        codes.add("{{total*1000|number_format(2, \".\", \"'\")}}                      ".trim());
        codes.add("{{ (iB.printed)?\"again \":\"\" }}                                 ".trim());
        codes.add("{{ (iB.printed)?myVarInsideInlineCond:SecondVarInsideInlineCond }} ".trim());
        codes.add("{{ iB.Name }}                                                      ".trim());
        codes.add("{% block inlineBlock %}                                            ".trim());
        codes.add("{{iB.Name}}                                                        ".trim());
        codes.add("{% block paragraphBlock %}                                         ".trim());
        codes.add("{{firstname}}                                                      ".trim());
        codes.add("{{lastname}}                                                       ".trim());
        codes.add("{{age}}                                                            ".trim());
        codes.add("{{firstname}}                                                      ".trim());
        codes.add("{%if (age<25) %}                                                   ".trim());
        codes.add("{%elseif (age>25 and age<32) %}                                    ".trim());
        codes.add("{%elseif (age>25 and age<32) %}                                    ".trim());
        codes.add("{% macro person (firstname, lastname, age) %}                      ".trim());
        codes.add("{% macro person (firstname, lastname, age) %}                      ".trim());
        codes.add("{% macro person (firstname, lastname, age) %}                      ".trim());
        codes.add("{% set input={Var1:10,Var2:25,Var3:1000,Var4:\"Text Value\"} %}    ".trim());
        codes.add("{% set total=0 %}                                                  ".trim());
        codes.add("{% set total=item+total %}                                         ".trim());
        codes.add("{% set total=item+total %}                                         ".trim());
        codes.add("{% set total=item+total %}                                         ".trim());
        codes.add("{% set total=0 %}                                                  ".trim());
        codes.add("{% set total=item+total %}                                         ".trim());
        codes.add("{% set total=item+total %}                                         ".trim());
        codes.add("{% set total=item+total %}                                         ".trim());
        codes.add("{% set iB={\"printed\":false,\"Name\":\"Steve\"} %}                ".trim());
        codes.add("{% set iB={\"Name\":\"John\"} %}                                   ".trim());
        codes.add("{{input.ImageFile1}}                                               ".trim());
        codes.add("{{loop.index0}}                                                    ".trim());
        codes.add("{{loop.index}}                                                     ".trim());
        codes.add("{{item}}                                                           ".trim());
        codes.add("{% for item in [1,2,3] %}                                          ".trim());
        codes.add("{{total*1000|number_format(2, \".\", \"'\")}}                      ".trim());
        codes.add("{% set total=0 %}                                                  ".trim());
        codes.add("{% set total=item+total %}                                         ".trim());
        codes.add("{% set total=item+total %}                                         ".trim());
        codes.add("{% set total=item+total %}                                         ".trim());
        for(String code : codes){
            parser.Parse(code);
        }
        Assert.assertEquals("[age, condition, condition2, easy, firstname, iB, iB.Name, iB.printed, inlineBlock, input, input.ImageFile1, input.Var1, input.Var2, input.Var3, input.Var4, item, lastname, loop.index, loop.index0, myVarInsideInlineCond, paragraphBlock, SecondVarInsideInlineCond, total, undefinedVariable]",parser.Vars().toString());
    }
}
