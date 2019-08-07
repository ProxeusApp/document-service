package com.proxeus.compiler.jtwig;

import com.google.common.base.Optional;

import org.jtwig.parser.addon.AddonParserProvider;
import org.jtwig.parser.config.DefaultSyntaxConfiguration;
import org.jtwig.parser.config.JtwigParserConfiguration;
import org.jtwig.parser.parboiled.expression.test.DefinedTestExpressionParser;
import org.jtwig.parser.parboiled.expression.test.DivisibleByTestExpressionParser;
import org.jtwig.parser.parboiled.expression.test.FunctionTestExpressionParser;
import org.jtwig.parser.parboiled.expression.test.IsFunctionTestExpressionParser;
import org.jtwig.parser.parboiled.expression.test.NullTestExpressionParser;
import org.jtwig.parser.parboiled.expression.test.SameAsTestExpressionParser;
import org.jtwig.parser.parboiled.expression.test.TestExpressionParser;
import org.jtwig.render.expression.calculator.operation.binary.impl.AndOperator;
import org.jtwig.render.expression.calculator.operation.binary.impl.CompositionOperator;
import org.jtwig.render.expression.calculator.operation.binary.impl.ConcatOperator;
import org.jtwig.render.expression.calculator.operation.binary.impl.DifferentOperator;
import org.jtwig.render.expression.calculator.operation.binary.impl.DivideOperator;
import org.jtwig.render.expression.calculator.operation.binary.impl.EquivalentOperator;
import org.jtwig.render.expression.calculator.operation.binary.impl.GreaterOperator;
import org.jtwig.render.expression.calculator.operation.binary.impl.GreaterOrEqualOperator;
import org.jtwig.render.expression.calculator.operation.binary.impl.InOperator;
import org.jtwig.render.expression.calculator.operation.binary.impl.IntDivideOperator;
import org.jtwig.render.expression.calculator.operation.binary.impl.IntMultiplyOperator;
import org.jtwig.render.expression.calculator.operation.binary.impl.LessOperator;
import org.jtwig.render.expression.calculator.operation.binary.impl.LessOrEqualOperator;
import org.jtwig.render.expression.calculator.operation.binary.impl.MatchesOperator;
import org.jtwig.render.expression.calculator.operation.binary.impl.ModOperator;
import org.jtwig.render.expression.calculator.operation.binary.impl.MultiplyOperator;
import org.jtwig.render.expression.calculator.operation.binary.impl.OrOperator;
import org.jtwig.render.expression.calculator.operation.binary.impl.SelectionOperator;
import org.jtwig.render.expression.calculator.operation.binary.impl.SubtractOperator;
import org.jtwig.render.expression.calculator.operation.binary.impl.SumOperator;
import org.jtwig.render.expression.calculator.operation.unary.impl.NegativeUnaryOperator;
import org.jtwig.render.expression.calculator.operation.unary.impl.NotUnaryOperator;

import java.util.Arrays;
import java.util.Collections;

import static java.util.Arrays.asList;

public class MyDefaultJtwigParserConfiguration extends JtwigParserConfiguration {

    public MyDefaultJtwigParserConfiguration() {
        super(new DefaultSyntaxConfiguration(),
                Collections.<AddonParserProvider>emptyList(),
                asList(
                        new NegativeUnaryOperator(),
                        new NotUnaryOperator()
                ),
                asList(
                        new MatchesOperator(),
                        new SelectionOperator(),
                        new CompositionOperator(),
                        new InOperator(),
                        new ConcatOperator(),

                        new SumOperator(),
                        new SubtractOperator(),
                        new IntDivideOperator(),
                        new IntMultiplyOperator(),
                        new DivideOperator(),
                        new MultiplyOperator(),
                        new ModOperator(),

                        new LessOrEqualOperator(),
                        new GreaterOrEqualOperator(),
                        new LessOperator(),
                        new GreaterOperator(),

                        new AndOperator(),
                        new OrOperator(),
                        new EquivalentOperator(),
                        new DifferentOperator()
                ),
                Arrays.<Class<? extends TestExpressionParser>>asList(
                        NullTestExpressionParser.class,
                        DefinedTestExpressionParser.class,
                        IsFunctionTestExpressionParser.class,
                        DivisibleByTestExpressionParser.class,
                        SameAsTestExpressionParser.class,
                        FunctionTestExpressionParser.class
                ),
                Optional.absent(),
                Collections.<String, Object>emptyMap());
    }
}
