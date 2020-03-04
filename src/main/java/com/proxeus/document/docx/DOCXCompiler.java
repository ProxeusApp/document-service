package com.proxeus.document.docx;

import com.proxeus.document.DocumentCompiler;
import com.proxeus.document.FileResult;
import com.proxeus.document.Template;
import com.proxeus.error.NotImplementedException;
import com.proxeus.office.microsoft.MicrosoftOfficeAssistant;
import com.proxeus.xml.template.TemplateHandlerFactory;

import java.util.Set;

/**
 * This is a placeholder.
 */
public class DOCXCompiler implements DocumentCompiler {
    private MicrosoftOfficeAssistant microsoftOfficeAssistant;
    private TemplateHandlerFactory templateHandlerFactory;

    public DOCXCompiler(String cacheFolder, MicrosoftOfficeAssistant msAssistant, TemplateHandlerFactory templateHandlerFactory) throws Exception {
        this.microsoftOfficeAssistant = msAssistant;
        this.templateHandlerFactory = templateHandlerFactory;

        //This is just a skeleton.  Code to be implemented as required.
        throw new NotImplementedException("DOCXCompiler not implemented");
    }

    public FileResult Compile(Template template) throws Exception {
        throw new NotImplementedException("not yet implemented");
    }

    public Set<String> Vars(Template template, String varPrefix) throws Exception {
        throw new NotImplementedException("not yet implemented");
    }
}
