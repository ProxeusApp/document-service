package com.proxeus.compiler.jtwig;

import com.google.common.collect.ImmutableMap;

import org.jtwig.macro.render.ImportRender;
import org.jtwig.model.expression.BinaryOperationExpression;
import org.jtwig.model.expression.ComprehensionListExpression;
import org.jtwig.model.expression.ConstantExpression;
import org.jtwig.model.expression.EnumeratedListExpression;
import org.jtwig.model.expression.Expression;
import org.jtwig.model.expression.FunctionExpression;
import org.jtwig.model.expression.MapExpression;
import org.jtwig.model.expression.MapSelectionExpression;
import org.jtwig.model.expression.TernaryOperationExpression;
import org.jtwig.model.expression.TestOperationExpression;
import org.jtwig.model.expression.UnaryOperationExpression;
import org.jtwig.model.expression.VariableExpression;
import org.jtwig.model.expression.test.DefinedTestExpression;
import org.jtwig.model.expression.test.DivisibleByTestExpression;
import org.jtwig.model.expression.test.FunctionTestExpression;
import org.jtwig.model.expression.test.IsFunctionTestExpression;
import org.jtwig.model.expression.test.NotTestExpression;
import org.jtwig.model.expression.test.NullTestExpression;
import org.jtwig.model.expression.test.SameAsTestExpression;
import org.jtwig.model.expression.test.TestExpression;
import org.jtwig.model.tree.AutoEscapeNode;
import org.jtwig.model.tree.BlockNode;
import org.jtwig.model.tree.CompositeNode;
import org.jtwig.model.tree.ContentEscapeNode;
import org.jtwig.model.tree.DoNode;
import org.jtwig.model.tree.EmbedNode;
import org.jtwig.model.tree.ExtendsNode;
import org.jtwig.model.tree.FilterNode;
import org.jtwig.model.tree.FlushNode;
import org.jtwig.model.tree.ForLoopNode;
import org.jtwig.model.tree.IfNode;
import org.jtwig.model.tree.ImportNode;
import org.jtwig.model.tree.ImportSelfNode;
import org.jtwig.model.tree.IncludeNode;
import org.jtwig.model.tree.MacroNode;
import org.jtwig.model.tree.Node;
import org.jtwig.model.tree.OutputNode;
import org.jtwig.model.tree.OverrideBlockNode;
import org.jtwig.model.tree.SetNode;
import org.jtwig.model.tree.TextNode;
import org.jtwig.model.tree.VerbatimNode;
import org.jtwig.render.config.RenderConfiguration;
import org.jtwig.render.expression.calculator.BinaryOperationExpressionCalculator;
import org.jtwig.render.expression.calculator.ComprehensionListExpressionCalculator;
import org.jtwig.render.expression.calculator.ConstantExpressionCalculator;
import org.jtwig.render.expression.calculator.EnumeratedListExpressionCalculator;
import org.jtwig.render.expression.calculator.ExpressionCalculator;
import org.jtwig.render.expression.calculator.FunctionArgumentsFactory;
import org.jtwig.render.expression.calculator.FunctionExpressionCalculator;
import org.jtwig.render.expression.calculator.MapExpressionCalculator;
import org.jtwig.render.expression.calculator.MapSelectionExpressionCalculator;
import org.jtwig.render.expression.calculator.TernaryExpressionCalculator;
import org.jtwig.render.expression.calculator.TestOperationExpressionCalculator;
import org.jtwig.render.expression.calculator.UnaryOperationExpressionCalculator;
import org.jtwig.render.expression.calculator.VariableExpressionCalculator;
import org.jtwig.render.expression.calculator.operation.binary.BinaryOperator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.AndOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.BinaryOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.BooleanOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.CompositionOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.ConcatOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.DifferentOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.DivideOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.EquivalentOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.GreaterOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.GreaterOrEqualOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.InOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.IntegerDivideOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.IntegerMultiplyOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.LessOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.LessOrEqualOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.MatchesOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.MathOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.ModOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.MultiplyOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.OrOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.SimpleOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.SubtractOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.SumOperationCalculator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.selection.SelectionErrorMessageGenerator;
import org.jtwig.render.expression.calculator.operation.binary.calculators.selection.SelectionOperationCalculator;
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
import org.jtwig.render.expression.calculator.operation.unary.UnaryOperator;
import org.jtwig.render.expression.calculator.operation.unary.calculators.NegativeOperationCalculator;
import org.jtwig.render.expression.calculator.operation.unary.calculators.NotOperationCalculator;
import org.jtwig.render.expression.calculator.operation.unary.calculators.UnaryOperationCalculator;
import org.jtwig.render.expression.calculator.operation.unary.impl.NegativeUnaryOperator;
import org.jtwig.render.expression.calculator.operation.unary.impl.NotUnaryOperator;
import org.jtwig.render.expression.test.calculator.DefinedTestExpressionCalculator;
import org.jtwig.render.expression.test.calculator.DivisibleByTestExpressionCalculator;
import org.jtwig.render.expression.test.calculator.FunctionTestExpressionCalculator;
import org.jtwig.render.expression.test.calculator.IsFunctionTestExpressionCalculator;
import org.jtwig.render.expression.test.calculator.NotTestExpressionCalculator;
import org.jtwig.render.expression.test.calculator.NullTestExpressionCalculator;
import org.jtwig.render.expression.test.calculator.SameAsTestExpressionCalculator;
import org.jtwig.render.expression.test.calculator.TestExpressionCalculator;
import org.jtwig.render.listeners.StagedRenderListener;
import org.jtwig.render.node.renderer.AutoEscapeNodeRender;
import org.jtwig.render.node.renderer.BlockNodeRender;
import org.jtwig.render.node.renderer.CompositeNodeRender;
import org.jtwig.render.node.renderer.ContentEscapeNodeRender;
import org.jtwig.render.node.renderer.DoNodeRender;
import org.jtwig.render.node.renderer.EmbedNodeRender;
import org.jtwig.render.node.renderer.ExtendsNodeRender;
import org.jtwig.render.node.renderer.FilterNodeRender;
import org.jtwig.render.node.renderer.FlushNodeRender;
import org.jtwig.render.node.renderer.ForLoopNodeRender;
import org.jtwig.render.node.renderer.IfNodeRender;
import org.jtwig.render.node.renderer.ImportNodeRender;
import org.jtwig.render.node.renderer.ImportSelfNodeRender;
import org.jtwig.render.node.renderer.IncludeNodeRender;
import org.jtwig.render.node.renderer.MacroNodeRender;
import org.jtwig.render.node.renderer.NodeRender;
import org.jtwig.render.node.renderer.OutputNodeRender;
import org.jtwig.render.node.renderer.OverrideBlockNodeRender;
import org.jtwig.render.node.renderer.SetNodeRender;
import org.jtwig.render.node.renderer.TextNodeRender;
import org.jtwig.render.node.renderer.VerbatimNodeRender;

import java.nio.charset.Charset;
import java.util.ArrayList;

public class MyRenderConfiguration extends RenderConfiguration {
    public MyRenderConfiguration(boolean strictMode) {
        super(strictMode,
                Charset.defaultCharset(),
                ImmutableMap.<Class<? extends Node>, NodeRender>builder()
                        .put(AutoEscapeNode.class, new AutoEscapeNodeRender())
                        .put(ContentEscapeNode.class, new ContentEscapeNodeRender())
                        .put(BlockNode.class, new BlockNodeRender())
                        .put(CompositeNode.class, new CompositeNodeRender())
                        .put(DoNode.class, new DoNodeRender())
                        .put(ExtendsNode.class, new ExtendsNodeRender())
                        .put(FilterNode.class, new FilterNodeRender())
                        .put(FlushNode.class, new FlushNodeRender())
                        .put(ForLoopNode.class, new ForLoopNodeRender())
                        .put(IfNode.class, new IfNodeRender())
                        .put(ImportSelfNode.class, new ImportSelfNodeRender(ImportRender.instance()))
                        .put(ImportNode.class, new ImportNodeRender(ImportRender.instance()))
                        .put(IncludeNode.class, new IncludeNodeRender())
                        .put(MacroNode.class, new MacroNodeRender())
                        .put(OutputNode.class, new OutputNodeRender())
                        .put(OverrideBlockNode.class, new OverrideBlockNodeRender())
                        .put(SetNode.class, new SetNodeRender())
                        .put(TextNode.class, new TextNodeRender())
                        .put(VerbatimNode.class, new VerbatimNodeRender())
                        .put(EmbedNode.class, new EmbedNodeRender())
                        .build(),

                ImmutableMap.<Class<? extends Expression>, ExpressionCalculator>builder()
                        .put(ConstantExpression.class, new ConstantExpressionCalculator())
                        .put(VariableExpression.class, new VariableExpressionCalculator())
                        .put(BinaryOperationExpression.class, new BinaryOperationExpressionCalculator())
                        .put(FunctionExpression.class, new FunctionExpressionCalculator(new FunctionArgumentsFactory()))
                        .put(MapExpression.class, new MapExpressionCalculator())
                        .put(ComprehensionListExpression.class, new ComprehensionListExpressionCalculator())
                        .put(EnumeratedListExpression.class, new EnumeratedListExpressionCalculator())
                        .put(MapSelectionExpression.class, new MapSelectionExpressionCalculator())
                        .put(UnaryOperationExpression.class, new UnaryOperationExpressionCalculator())
                        .put(TernaryOperationExpression.class, new TernaryExpressionCalculator())
                        .put(TestOperationExpression.class, new TestOperationExpressionCalculator())
                        .build(),

                ImmutableMap.<Class<? extends BinaryOperator>, BinaryOperationCalculator>builder()
                        .put(MatchesOperator.class, new SimpleOperationCalculator(new MatchesOperationCalculator()))
                        .put(ConcatOperator.class, new SimpleOperationCalculator(new ConcatOperationCalculator()))
                        .put(CompositionOperator.class, new CompositionOperationCalculator())
                        .put(SelectionOperator.class, new SelectionOperationCalculator(new SelectionErrorMessageGenerator()))
                        .put(InOperator.class, new SimpleOperationCalculator(new InOperationCalculator()))

                        .put(SumOperator.class, new SimpleOperationCalculator(new MathOperationCalculator(new SumOperationCalculator())))
                        .put(SubtractOperator.class, new SimpleOperationCalculator(new MathOperationCalculator(new SubtractOperationCalculator())))
                        .put(DivideOperator.class, new SimpleOperationCalculator(new MathOperationCalculator(new DivideOperationCalculator())))
                        .put(MultiplyOperator.class, new SimpleOperationCalculator(new MathOperationCalculator(new MultiplyOperationCalculator())))
                        .put(IntDivideOperator.class, new SimpleOperationCalculator(new MathOperationCalculator(new IntegerDivideOperationCalculator())))
                        .put(IntMultiplyOperator.class, new SimpleOperationCalculator(new MathOperationCalculator(new IntegerMultiplyOperationCalculator())))
                        .put(ModOperator.class, new SimpleOperationCalculator(new MathOperationCalculator(new ModOperationCalculator())))

                        .put(EquivalentOperator.class, new SimpleOperationCalculator(new EquivalentOperationCalculator()))
                        .put(DifferentOperator.class, new SimpleOperationCalculator(new DifferentOperationCalculator()))
                        .put(LessOperator.class, new SimpleOperationCalculator(new LessOperationCalculator()))
                        .put(LessOrEqualOperator.class, new SimpleOperationCalculator(new LessOrEqualOperationCalculator()))
                        .put(GreaterOperator.class, new SimpleOperationCalculator(new GreaterOperationCalculator()))
                        .put(GreaterOrEqualOperator.class, new SimpleOperationCalculator(new GreaterOrEqualOperationCalculator()))

                        .put(AndOperator.class, new SimpleOperationCalculator(new BooleanOperationCalculator(new AndOperationCalculator())))
                        .put(OrOperator.class, new SimpleOperationCalculator(new BooleanOperationCalculator(new OrOperationCalculator())))
                        .build(),

                ImmutableMap.<Class<? extends UnaryOperator>, UnaryOperationCalculator>builder()
                        .put(NegativeUnaryOperator.class, new NegativeOperationCalculator())
                        .put(NotUnaryOperator.class, new NotOperationCalculator())
                        .build(),

                ImmutableMap.<Class<? extends TestExpression>, TestExpressionCalculator>builder()
                        .put(NotTestExpression.class, new NotTestExpressionCalculator())
                        .put(DefinedTestExpression.class, new DefinedTestExpressionCalculator())
                        .put(IsFunctionTestExpression.class, new IsFunctionTestExpressionCalculator())
                        .put(DivisibleByTestExpression.class, new DivisibleByTestExpressionCalculator())
                        .put(NullTestExpression.class, new NullTestExpressionCalculator())
                        .put(SameAsTestExpression.class, new SameAsTestExpressionCalculator())
                        .put(FunctionTestExpression.class, new FunctionTestExpressionCalculator())
                        .build(),
                new ArrayList<StagedRenderListener>());
    }


}