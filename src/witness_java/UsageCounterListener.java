package witness_java;

import myjava.JavaLexer;
import myjava.JavaParser;
import myjava.JavaParserBaseListener;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UsageCounterListener extends JavaParserBaseListener {

    private int targetLineNumber;
    private TokenStream tokens;
    Map<String, Integer> usageCount = new HashMap<>();

    public UsageCounterListener(int line, TokenStream tokens) {
        this.targetLineNumber = line;
        this.tokens = tokens;
    }

    private Set<String> extractVariables(JavaParser.ParExpressionContext ctx) {
        Set<String> variables = new HashSet<>();
        ParseTreeWalker walker = new ParseTreeWalker();
        VariableExtractorListener extractor = new VariableExtractorListener(variables);
        walker.walk(extractor, ctx);
        return variables;
    }

    private Set<String> extractVariables(JavaParser.ExpressionContext ctx) {
        Set<String> variables = new HashSet<>();
        ParseTreeWalker walker = new ParseTreeWalker();
        VariableExtractorListener extractor = new VariableExtractorListener(variables);
        walker.walk(extractor, ctx);
        return variables;
    }

    @Override
    public void enterMemberDeclaration(JavaParser.MemberDeclarationContext ctx) {
        if (ctx.getStart().getLine() <= targetLineNumber && ctx.getStop().getLine() >= targetLineNumber && ctx.getStart().getLine() == targetLineNumber) {

            // Check for field declaration (class-level variable)
            JavaParser.FieldDeclarationContext fieldCtx = ctx.getChild(JavaParser.FieldDeclarationContext.class, 0);
            if (fieldCtx != null) {
                JavaParser.VariableDeclaratorContext varCtx = fieldCtx.variableDeclarators().variableDeclarator(0);
                if (varCtx != null) {
                    String variableName = varCtx.variableDeclaratorId().getText();
                    System.out.println("Declared variable: " + variableName);

                    boolean foundTarget = false;
                    usageCount.put(variableName, 0);

                    // Get the start and stop tokens for the current context
                    int startTokenIndex = ctx.getStart().getTokenIndex();
                    int stopTokenIndex = ctx.getStop().getTokenIndex();
                    Interval targetInterval = Interval.of(startTokenIndex, stopTokenIndex);

                    for (int i = 0; i < ctx.getParent().getParent().getChildCount(); i++) {
                        ParseTree child = ctx.getParent().getParent().getChild(i);
                        // Extract the source interval of the child node
                        Interval childInterval = child.getSourceInterval();

                        // Check if the child's interval matches the target, allowing for a semicolon difference
                        if (childInterval.a <= targetInterval.a && targetInterval.b <= childInterval.b) {
                            foundTarget = true;
                            continue; // Skip the target line itself
                        }

                        if (foundTarget) {
                            if (child instanceof ParserRuleContext) {
                                for (String var : new HashSet<>(usageCount.keySet())) {
                                    // Create a new counter for each variable
                                    VariableUsageCounter counter = new VariableUsageCounter(var);

                                    // Walk the parse tree starting from the child node
                                    ParseTreeWalker walker = new ParseTreeWalker();
                                    walker.walk(counter, child); // child is the current statement or expression

                                    // Get the count of occurrences for the current variable
                                    usageCount.put(var, usageCount.get(var) + counter.getCount());
                                }
                            }
                        }

                    }
                }
            }

        }else if (ctx.getStart().getLine() <= targetLineNumber && ctx.getStop().getLine() >= targetLineNumber && ctx.methodDeclaration() == null) {

            // Check for field declaration (class-level variable)
            JavaParser.FieldDeclarationContext fieldCtx = ctx.getChild(JavaParser.FieldDeclarationContext.class, 0);
            if (fieldCtx != null) {
                JavaParser.VariableDeclaratorContext varCtx = fieldCtx.variableDeclarators().variableDeclarator(0);
                if (varCtx != null) {
                    String variableName = varCtx.variableDeclaratorId().getText();

                    boolean foundTarget = false;
                    usageCount.put(variableName, 0);

                    // Get the start and stop tokens for the current context
                    int startTokenIndex = ctx.getStart().getTokenIndex();
                    int stopTokenIndex = ctx.getStop().getTokenIndex();
                    Interval targetInterval = Interval.of(startTokenIndex, stopTokenIndex);

                    for (int i = 0; i < ctx.getParent().getParent().getChildCount(); i++) {
                        ParseTree child = ctx.getParent().getParent().getChild(i);
                        // Extract the source interval of the child node
                        Interval childInterval = child.getSourceInterval();

                        // Check if the child's interval matches the target, allowing for a semicolon difference
                        if (childInterval.a <= targetInterval.a && targetInterval.b <= childInterval.b) {
                            foundTarget = true;
                            continue; // Skip the target line itself
                        }

                        if (foundTarget) {
                            if (child instanceof ParserRuleContext) {
                                Interval interval = child.getSourceInterval();
                                String text = tokens.getText(interval);

                                for (String var : new HashSet<>(usageCount.keySet())) {
                                    // Create a new counter for each variable
                                    VariableUsageCounter counter = new VariableUsageCounter(var);

                                    // Walk the parse tree starting from the child node
                                    ParseTreeWalker walker = new ParseTreeWalker();
                                    walker.walk(counter, child); // child is the current statement or expression

                                    // Get the count of occurrences for the current variable
                                    usageCount.put(var, usageCount.get(var) + counter.getCount());
                                }
                            }
                        }

                    }
                }
            }

        }

    }

    @Override
    public void enterLocalVariableDeclaration(JavaParser.LocalVariableDeclarationContext ctx) {
        if (ctx.getStart().getLine() <= targetLineNumber && ctx.getStop().getLine() >= targetLineNumber && ctx.getStart().getLine() == targetLineNumber) {
            // Check if the parent context is not a forInit
            if (!(ctx.getParent() instanceof JavaParser.ForInitContext)) {

                // Extract the variable name(s)
                for (JavaParser.VariableDeclaratorContext varDeclCtx : ctx.variableDeclarators().variableDeclarator()) {
                    String variableName = varDeclCtx.variableDeclaratorId().getText();

                    boolean foundTarget = false;
                    usageCount.put(variableName, 0);

                    // Get the start and stop tokens for the current context
                    int startTokenIndex = ctx.getStart().getTokenIndex();
                    int stopTokenIndex = ctx.getStop().getTokenIndex();
                    Interval targetInterval = Interval.of(startTokenIndex, stopTokenIndex);

                    for (int i = 0; i < ctx.getParent().getParent().getChildCount(); i++) {
                        ParseTree child = ctx.getParent().getParent().getChild(i);
                        // Extract the source interval of the child node
                        Interval childInterval = child.getSourceInterval();

                        // Check if the child's interval matches the target, allowing for a semicolon difference
                        if (childInterval.a == targetInterval.a && (childInterval.b == targetInterval.b || childInterval.b == targetInterval.b + 1)) {
                            foundTarget = true;
                            continue; // Skip the target line itself
                        }

                        if (foundTarget) {
                            if (child instanceof ParserRuleContext) {
                                for (String var : new HashSet<>(usageCount.keySet())) {
                                    // Create a new counter for each variable
                                    VariableUsageCounter counter = new VariableUsageCounter(var);

                                    // Walk the parse tree starting from the child node
                                    ParseTreeWalker walker = new ParseTreeWalker();
                                    walker.walk(counter, child); // child is the current statement or expression

                                    // Get the count of occurrences for the current variable
                                    usageCount.put(var, usageCount.get(var) + counter.getCount());
                                }
                            }
                        }
                    }
                }

            }
        }else if (ctx.getStart().getLine() <= targetLineNumber && ctx.getStop().getLine() >= targetLineNumber) {

            // Check if the parent context is not a forInit
            if (!(ctx.getParent() instanceof JavaParser.ForInitContext)) {

                // Extract the variable name(s)
                for (JavaParser.VariableDeclaratorContext varDeclCtx : ctx.variableDeclarators().variableDeclarator()) {
                    String variableName = varDeclCtx.variableDeclaratorId().getText();

                    boolean foundTarget = false;
                    usageCount.put(variableName, 0);

                    // Get the start and stop tokens for the current context
                    int startTokenIndex = ctx.getStart().getTokenIndex();
                    int stopTokenIndex = ctx.getStop().getTokenIndex();
                    Interval targetInterval = Interval.of(startTokenIndex, stopTokenIndex);

                    for (int i = 0; i < ctx.getParent().getParent().getChildCount(); i++) {
                        ParseTree child = ctx.getParent().getParent().getChild(i);
                        // Extract the source interval of the child node
                        Interval childInterval = child.getSourceInterval();

                        // Check if the child's interval matches the target, allowing for a semicolon difference
                        if (childInterval.a == targetInterval.a && (childInterval.b == targetInterval.b || childInterval.b == targetInterval.b + 1)) {
                            foundTarget = true;
                            continue; // Skip the target line itself
                        }

                        if (foundTarget) {
                            if (child instanceof ParserRuleContext) {
                                Interval interval = child.getSourceInterval();
                                String text = tokens.getText(interval);

                                for (String var : new HashSet<>(usageCount.keySet())) {
                                    // Create a new counter for each variable
                                    VariableUsageCounter counter = new VariableUsageCounter(var);

                                    // Walk the parse tree starting from the child node
                                    ParseTreeWalker walker = new ParseTreeWalker();
                                    walker.walk(counter, child); // child is the current statement or expression

                                    // Get the count of occurrences for the current variable
                                    usageCount.put(var, usageCount.get(var) + counter.getCount());
                                }
                            }
                        }
                    }
                }

            }

        }
    }

    @Override
    public void enterStatement(JavaParser.StatementContext ctx) {
        if (ctx.getStart().getLine() <= targetLineNumber && targetLineNumber <= ctx.getStop().getLine() && targetLineNumber == ctx.getStart().getLine()) {
            if (ctx.IF() != null || ctx.WHILE() != null  || ctx.SWITCH() != null) {
                Set<String> variables = extractVariables(ctx.parExpression());
                for (String var : variables) {
                    usageCount.put(var, 0);
                }

                for (JavaParser.StatementContext statementContext : ctx.statement()) {
                    for (int i = 0; i < statementContext.getChildCount(); i++) {
                        for (int j = 0; j < statementContext.getChild(i).getChildCount(); j++) {
                            ParseTree child = statementContext.getChild(i).getChild(j);
                            if (child instanceof ParserRuleContext) {
                                Interval interval = child.getSourceInterval();
                                String text = tokens.getText(interval);
                                for (String var : new HashSet<>(usageCount.keySet())) {
                                    // Create a new counter for each variable
                                    VariableUsageCounter counter = new VariableUsageCounter(var);

                                    // Walk the parse tree starting from the child node
                                    ParseTreeWalker walker = new ParseTreeWalker();
                                    walker.walk(counter, child); // child is the current statement or expression

                                    // Get the count of occurrences for the current variable
                                    usageCount.put(var, usageCount.get(var) + counter.getCount());
                                }
                            }
                        }
                    }
                }
                return;
            }

            if (ctx.FOR() != null) {
                JavaParser.ForControlContext forCtrl = ctx.forControl();
                Set<String> variables = Set.of();
                if (forCtrl.enhancedForControl() != null) {
                    variables = extractVariables(forCtrl.enhancedForControl().expression());
                } else if (forCtrl.expression() != null) {
                    variables = extractVariables(forCtrl.expression());
                }

                for (String var : variables) {
                    usageCount.put(var, 0);
                }

                for (JavaParser.StatementContext statementContext : ctx.statement()) {
                    for (int i = 0; i < statementContext.getChildCount(); i++) {
                        for (int j = 0; j < statementContext.getChild(i).getChildCount(); j++) {
                            ParseTree child = statementContext.getChild(i).getChild(j);
                            if (child instanceof ParserRuleContext) {
                                Interval interval = child.getSourceInterval();
                                String text = tokens.getText(interval);
                                for (String var : new HashSet<>(usageCount.keySet())) {
                                    // Create a new counter for each variable
                                    VariableUsageCounter counter = new VariableUsageCounter(var);

                                    // Walk the parse tree starting from the child node
                                    ParseTreeWalker walker = new ParseTreeWalker();
                                    walker.walk(counter, child); // child is the current statement or expression

                                    // Get the count of occurrences for the current variable
                                    usageCount.put(var, usageCount.get(var) + counter.getCount());
                                }
                            }
                        }
                    }
                }

                return;
            }

            if (ctx.statementExpression != null) {
                if (ctx.statementExpression.ASSIGN() != null || ctx.statementExpression.INC() != null || ctx.statementExpression.DEC() != null
                        || ctx.statementExpression.AND_ASSIGN() != null || ctx.statementExpression.OR_ASSIGN() != null || ctx.statementExpression.XOR_ASSIGN() != null
                        || ctx.statementExpression.ADD_ASSIGN() != null || ctx.statementExpression.SUB_ASSIGN() != null
                        || ctx.statementExpression.MUL_ASSIGN() != null || ctx.statementExpression.DIV_ASSIGN() != null
                        || ctx.statementExpression.MOD_ASSIGN() != null
                        || ctx.statementExpression.LSHIFT_ASSIGN() != null || ctx.statementExpression.RSHIFT_ASSIGN() != null || ctx.statementExpression.URSHIFT_ASSIGN() != null) { // Must not change it
                    String assignedVariable = ctx.statementExpression.expression(0).getText();

                    boolean foundTarget = false;
                    usageCount.put(assignedVariable, 0);

                    Interval targetInterval = null;
                    if (ctx.statementExpression.ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.INC() != null) {
                        targetInterval = ctx.statementExpression.INC().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.DEC() != null) {
                        targetInterval = ctx.statementExpression.DEC().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.AND_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.AND_ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.OR_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.OR_ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.XOR_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.XOR_ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.ADD_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.ADD_ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.SUB_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.SUB_ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.MUL_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.MUL_ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.DIV_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.DIV_ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.MOD_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.MOD_ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.LSHIFT_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.LSHIFT_ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.RSHIFT_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.RSHIFT_ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.URSHIFT_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.URSHIFT_ASSIGN().getParent().getSourceInterval();
                    }

                    for (int i = 0; i < ctx.getParent().getParent().getChildCount(); i++) {
                        ParseTree child = ctx.getParent().getParent().getChild(i);
                        // Extract the source interval of the child node
                        Interval childInterval = child.getSourceInterval();

                        // Check if the child's interval matches the target, allowing for a semicolon difference
                        if (childInterval.a == targetInterval.a && (childInterval.b == targetInterval.b || childInterval.b == targetInterval.b + 1)) {
                            foundTarget = true;
                            continue; // Skip the target line itself
                        }

                        if (foundTarget) {
                            if (child instanceof ParserRuleContext) {
                                Interval interval = child.getSourceInterval();
                                String text = tokens.getText(interval);

                                for (String var : new HashSet<>(usageCount.keySet())) {
                                    // Create a new counter for each variable
                                    VariableUsageCounter counter = new VariableUsageCounter(var);

                                    // Walk the parse tree starting from the child node
                                    ParseTreeWalker walker = new ParseTreeWalker();
                                    walker.walk(counter, child); // child is the current statement or expression

                                    // Get the count of occurrences for the current variable
                                    usageCount.put(var, usageCount.get(var) + counter.getCount());
                                }
                            }
                        }

                    }

                }
            }
        }else if (ctx.getStart().getLine() <= targetLineNumber && targetLineNumber <= ctx.getStop().getLine() && targetLineNumber == ctx.getStop().getLine()) {
            if (ctx.WHILE() != null) {
                Set<String> variables = extractVariables(ctx.parExpression());
                for (String var : variables) {
                    usageCount.put(var, 0);
                }

                for (JavaParser.StatementContext statementContext : ctx.statement()) {
                    for (int i = 0; i < statementContext.getChildCount(); i++) {
                        for (int j = 0; j < statementContext.getChild(i).getChildCount(); j++) {
                            ParseTree child = statementContext.getChild(i).getChild(j);
                            if (child instanceof ParserRuleContext) {
                                Interval interval = child.getSourceInterval();
                                String text = tokens.getText(interval);
                                for (String var : new HashSet<>(usageCount.keySet())) {
                                    // Create a new counter for each variable
                                    VariableUsageCounter counter = new VariableUsageCounter(var);

                                    // Walk the parse tree starting from the child node
                                    ParseTreeWalker walker = new ParseTreeWalker();
                                    walker.walk(counter, child); // child is the current statement or expression

                                    // Get the count of occurrences for the current variable
                                    usageCount.put(var, usageCount.get(var) + counter.getCount());
                                }
                            }
                        }
                    }
                }

            }
        } else if (ctx.getStart().getLine() <= targetLineNumber && ctx.getStop().getLine() >= targetLineNumber) {
            if (ctx.IF() != null) {
                Token start = ctx.IF().getSymbol();
                Token stop = ctx.parExpression().stop;

                if (start != null && stop != null) {
                    int startLine = start.getLine();
                    int stopLine = stop.getLine();

                    // Assuming `givenLineNumber` is the line number you want to check
                    if (targetLineNumber >= startLine && targetLineNumber <= stopLine) {
                        Set<String> variables = extractVariables(ctx.parExpression());
                        for (String var : variables) {
                            usageCount.put(var, 0);
                        }

                        for (JavaParser.StatementContext statementContext : ctx.statement()) {
                            for (int i = 0; i < statementContext.getChildCount(); i++) {
                                for (int j = 0; j < statementContext.getChild(i).getChildCount(); j++) {
                                    ParseTree child = statementContext.getChild(i).getChild(j);
                                    if (child instanceof ParserRuleContext) {
                                        Interval interval = child.getSourceInterval();
                                        String text = tokens.getText(interval);
                                        for (String var : new HashSet<>(usageCount.keySet())) {
                                            // Create a new counter for each variable
                                            VariableUsageCounter counter = new VariableUsageCounter(var);

                                            // Walk the parse tree starting from the child node
                                            ParseTreeWalker walker = new ParseTreeWalker();
                                            walker.walk(counter, child); // child is the current statement or expression

                                            // Get the count of occurrences for the current variable
                                            usageCount.put(var, usageCount.get(var) + counter.getCount());
                                        }
                                    }
                                }
                            }
                        }

                    }
                }

                return;
            }
            if (ctx.WHILE() != null) {
                Token start = ctx.WHILE().getSymbol();
                Token stop = ctx.parExpression().stop;

                if (start != null && stop != null) {
                    int startLine = start.getLine();
                    int stopLine = stop.getLine();

                    // Assuming `targetLineNumber` is the line number you want to check
                    if (targetLineNumber >= startLine && targetLineNumber <= stopLine) {
                        Set<String> variables = extractVariables(ctx.parExpression());
                        for (String var : variables) {
                            usageCount.put(var, 0);
                        }

                        for (JavaParser.StatementContext statementContext : ctx.statement()) {
                            for (int i = 0; i < statementContext.getChildCount(); i++) {
                                for (int j = 0; j < statementContext.getChild(i).getChildCount(); j++) {
                                    ParseTree child = statementContext.getChild(i).getChild(j);
                                    if (child instanceof ParserRuleContext) {
                                        Interval interval = child.getSourceInterval();
                                        String text = tokens.getText(interval);
                                        for (String var : new HashSet<>(usageCount.keySet())) {
                                            // Create a new counter for each variable
                                            VariableUsageCounter counter = new VariableUsageCounter(var);

                                            // Walk the parse tree starting from the child node
                                            ParseTreeWalker walker = new ParseTreeWalker();
                                            walker.walk(counter, child); // child is the current statement or expression

                                            // Get the count of occurrences for the current variable
                                            usageCount.put(var, usageCount.get(var) + counter.getCount());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return;
            }

            if (ctx.statementExpression != null) {
                if (ctx.statementExpression.ASSIGN() != null || ctx.statementExpression.INC() != null || ctx.statementExpression.DEC() != null
                        || ctx.statementExpression.AND_ASSIGN() != null || ctx.statementExpression.OR_ASSIGN() != null || ctx.statementExpression.XOR_ASSIGN() != null
                        || ctx.statementExpression.ADD_ASSIGN() != null || ctx.statementExpression.SUB_ASSIGN() != null
                        || ctx.statementExpression.MUL_ASSIGN() != null || ctx.statementExpression.DIV_ASSIGN() != null
                        || ctx.statementExpression.MOD_ASSIGN() != null
                        || ctx.statementExpression.LSHIFT_ASSIGN() != null || ctx.statementExpression.RSHIFT_ASSIGN() != null || ctx.statementExpression.URSHIFT_ASSIGN() != null) {
                    String assignedVariable = ctx.statementExpression.expression(0).getText();

                    boolean foundTarget = false;
                    usageCount.put(assignedVariable, 0);

                    Interval targetInterval = null;
                    if (ctx.statementExpression.ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.INC() != null) {
                        targetInterval = ctx.statementExpression.INC().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.DEC() != null) {
                        targetInterval = ctx.statementExpression.DEC().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.AND_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.AND_ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.OR_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.OR_ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.XOR_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.XOR_ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.ADD_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.ADD_ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.SUB_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.SUB_ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.MUL_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.MUL_ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.DIV_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.DIV_ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.MOD_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.MOD_ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.LSHIFT_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.LSHIFT_ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.RSHIFT_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.RSHIFT_ASSIGN().getParent().getSourceInterval();
                    }
                    if (ctx.statementExpression.URSHIFT_ASSIGN() != null) {
                        targetInterval = ctx.statementExpression.URSHIFT_ASSIGN().getParent().getSourceInterval();
                    }

                    for (int i = 0; i < ctx.getParent().getParent().getChildCount(); i++) {
                        ParseTree child = ctx.getParent().getParent().getChild(i);
                        // Extract the source interval of the child node
                        Interval childInterval = child.getSourceInterval();

                        // Check if the child's interval matches the target, allowing for a semicolon difference
                        if (childInterval.a == targetInterval.a && (childInterval.b == targetInterval.b || childInterval.b == targetInterval.b + 1)) {
                            foundTarget = true;
                            continue; // Skip the target line itself
                        }

                        if (foundTarget) {
                            if (child instanceof ParserRuleContext) {
                                Interval interval = child.getSourceInterval();
                                String text = tokens.getText(interval);

                                for (String var : new HashSet<>(usageCount.keySet())) {
                                    // Create a new counter for each variable
                                    VariableUsageCounter counter = new VariableUsageCounter(var);

                                    // Walk the parse tree starting from the child node
                                    ParseTreeWalker walker = new ParseTreeWalker();
                                    walker.walk(counter, child); // child is the current statement or expression

                                    // Get the count of occurrences for the current variable
                                    usageCount.put(var, usageCount.get(var) + counter.getCount());
                                }
                            }
                        }

                    }

                }
            }
        }
    }

    class VariableExtractorListener extends JavaParserBaseListener {
        private final Set<String> variables;

        public VariableExtractorListener(Set<String> variables) {
            this.variables = variables;
        }

        @Override
        public void enterPrimary(JavaParser.PrimaryContext ctx) {
            if (ctx.identifier() != null) {
                variables.add(ctx.identifier().getText());
            }
        }
    }

    class VariableUsageCounter extends JavaParserBaseListener {
        private final String variable;
        private int count = 0;
        private boolean isReassigned = false; // To track if the variable is reassigned

        public VariableUsageCounter(String variable) {
            this.variable = variable;
        }

        public int getCount() {
            return count;
        }

        @Override
        public void enterPrimary(JavaParser.PrimaryContext ctx) {
            // Count occurrences of the variable if it hasn't been reassigned
            if (!isReassigned && ctx.identifier() != null && ctx.identifier().getText().equals(variable)) {
                count++;
            }
        }
    }

}

