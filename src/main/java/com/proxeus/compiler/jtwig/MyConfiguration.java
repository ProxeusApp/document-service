package com.proxeus.compiler.jtwig;

import com.google.common.collect.ImmutableMap;

import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.initializer.EnvironmentInitializer;
import org.jtwig.escape.EscapeEngine;
import org.jtwig.escape.JavascriptEscapeEngine;
import org.jtwig.escape.config.EscapeEngineConfiguration;
import org.jtwig.extension.Extension;
import org.jtwig.functions.config.DefaultJtwigFunctionList;
import org.jtwig.property.configuration.DefaultPropertyResolverConfiguration;
import org.jtwig.render.expression.calculator.enumerated.config.DefaultEnumerationListStrategyList;
import org.jtwig.value.config.DefaultValueConfiguration;

import java.util.Collections;

public class MyConfiguration extends EnvironmentConfiguration {
    public MyConfiguration(boolean strictMode) {
        super(
                new MyResourceConfiguration(),
                new DefaultEnumerationListStrategyList(),
                //new DefaultJtwigParserConfiguration(), this is using cache, we don't need it
                new MyDefaultJtwigParserConfiguration(),
                new DefaultValueConfiguration(),
                new MyRenderConfiguration(strictMode),
                new EscapeEngineConfiguration("html",
                        "html",
                        ImmutableMap.<String, EscapeEngine>builder()
                                .put("html", MyXMLEscapeEngine.instance())
                                .put("js", JavascriptEscapeEngine.instance())
                                .put("javascript", JavascriptEscapeEngine.instance())
                                .build()),
                new DefaultPropertyResolverConfiguration(),
                new DefaultJtwigFunctionList(),
                Collections.<String, Object>emptyMap(),
                Collections.<Extension>emptyList(),
                Collections.<EnvironmentInitializer>emptyList());
    }
}

