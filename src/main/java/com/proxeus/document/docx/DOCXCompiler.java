package com.proxeus.document.docx;

import com.proxeus.compiler.jtwig.MyJTwigCompiler;
import com.proxeus.document.DocumentCompilerIF;
import com.proxeus.document.FileResult;
import com.proxeus.document.Template;
import com.proxeus.error.NotImplementedException;
import com.proxeus.office.microsoft.MicrosoftOfficeAssistant;
import com.proxeus.xml.template.TemplateHandlerFactory;

import java.util.Set;

public class DOCXCompiler implements DocumentCompilerIF {
    private MyJTwigCompiler compiler;
    private MicrosoftOfficeAssistant microsoftOfficeAssistant;
    private TemplateHandlerFactory templateHandlerFactory;

    public DOCXCompiler(String cacheFolder, MyJTwigCompiler compiler, MicrosoftOfficeAssistant msAssistant, TemplateHandlerFactory templateHandlerFactory) throws Exception {
        this.microsoftOfficeAssistant = msAssistant;
        this.compiler = compiler;
        this.templateHandlerFactory = templateHandlerFactory;
        //TODO impl. if demanded
    }

    public FileResult Compile(Template template) throws Exception {
        throw new NotImplementedException("not yet implemented");
    }

    public Set<String> Vars(Template template, String varPrefix) throws Exception {
        throw new NotImplementedException("not yet implemented");
    }
}
