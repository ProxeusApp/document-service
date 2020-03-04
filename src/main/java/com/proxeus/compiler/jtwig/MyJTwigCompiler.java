package com.proxeus.compiler.jtwig;

import com.proxeus.error.CompilationException;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.jtwig.environment.Environment;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentFactory;
import org.jtwig.escape.environment.EscapeEnvironmentFactory;
import org.jtwig.functions.environment.FunctionResolverFactory;
import org.jtwig.parser.JtwigParserFactory;
import org.jtwig.property.environment.PropertyResolverEnvironmentFactory;
import org.jtwig.render.environment.RenderEnvironmentFactory;
import org.jtwig.render.expression.calculator.enumerated.environment.EnumerationListStrategyFactory;
import org.jtwig.renderable.RenderResult;
import org.jtwig.renderable.StreamRenderResult;
import org.jtwig.resource.reference.ResourceReference;
import org.jtwig.value.environment.ValueEnvironmentFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * MyJTwigCompiler helps us to configure jTwig as we need it for XML documents.
 */
public class MyJTwigCompiler {
    private EnvironmentConfiguration configuration = new MyConfiguration(false);
    private EnvironmentFactory environmentFactory = new EnvironmentFactory(new JtwigParserFactory(), new MyResourceEnvironmentFactory(),
            new RenderEnvironmentFactory(),
            new FunctionResolverFactory(),
            new PropertyResolverEnvironmentFactory(),
            new ValueEnvironmentFactory(),
            new EnumerationListStrategyFactory(),
            new EscapeEnvironmentFactory());
    private Environment environment = environmentFactory.create(configuration);
    private MyJTwigUserReadableErrorBeautifier errorBeautifier = new MyJTwigUserReadableErrorBeautifier(1400);
    private Method renderMethodWithCharset;

    public MyJTwigCompiler(){
        try{
            /**
             * The public method of JTwig get the default charset from the environment, which makes it impossible to reuse in multiple threads.
             * That's why we call the private method to provide the current charset in the method call.
             */
            renderMethodWithCharset = JtwigTemplate.class.getDeclaredMethod("render", JtwigModel.class, RenderResult.class);
            renderMethodWithCharset.setAccessible(true);
        }catch (NoSuchMethodException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void Compile(InputStream inputStream, Map<String, Object> data, OutputStream outputStream, Charset charset) throws Exception {
        try{
            ResourceReference rr = new MyResourceReferenceInlineInputStream(inputStream);
            JtwigTemplate tmpl = new JtwigTemplate(environment, rr);
            renderMethodWithCharset.invoke(tmpl, JtwigModel.newModel(data), new StreamRenderResult(outputStream, charset));
        }catch (Exception e){
            String readableErrorString = errorBeautifier.FormatError(inputStream, charset, e);
            if(readableErrorString!=null){
                throw new CompilationException(readableErrorString);
            }
            throw new CompilationException(e.getMessage(),e);
        }
    }
}
