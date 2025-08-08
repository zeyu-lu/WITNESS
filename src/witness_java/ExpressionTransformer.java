package witness_java;

import myjava.JavaParser;
import myjava.JavaParserBaseVisitor;

class ExpressionTransformer extends JavaParserBaseVisitor<String> {

    private int exprCounter = 0;
    private boolean isEqual;

    public ExpressionTransformer(boolean isEqual) {
        this.isEqual = isEqual;
    }

    @Override
    public String visitExpression(JavaParser.ExpressionContext ctx) {
        // Handle binary expressions with logical operators (&&, ||)
        if (ctx.bop != null && (ctx.bop.getType() == JavaParser.AND || ctx.bop.getType() == JavaParser.OR)) {
            String left = visit(ctx.expression(0));
            String right = visit(ctx.expression(1));

            if (isLogicalExpression(ctx.expression(0)) || isLogicalExpression(ctx.expression(1))) {
                return left + " " + ctx.bop.getText() + " " + right;
            } else {
                return left + " " + ctx.bop.getText() + " " + right;
            }
        }
        // Handle binary expressions with relational operators (==, !=, >, <, >=, <=)
        else if (ctx.bop != null && ctx.expression().size() == 2) {
            if (!isEqual) {
                return "expr" + (++exprCounter);
            } else {
                return "(" + "expr" + (++exprCounter) + " " + ctx.bop.getText() + " expr" + (++exprCounter) + ")";
            }
        }
        // For primary expressions (to handle parentheses)
        else if (ctx.primary() != null) {
            return visit(ctx.primary());
        }
        return "expr" + (++exprCounter);
    }

    private boolean isLogicalExpression(JavaParser.ExpressionContext ctx) {
        return ctx.bop != null && (ctx.bop.getType() == JavaParser.AND || ctx.bop.getType() == JavaParser.OR);
    }

    @Override
    public String visitPrimary(JavaParser.PrimaryContext ctx) {
        // Specifically handle parentheses
        if (ctx.expression() != null) {
            return "(" + visit(ctx.expression()) + ")";
        }
        return "expr" + (++exprCounter);
    }
}