package witness_java;

import myjava.JavaLexer;
import myjava.JavaParser;
import myjava.JavaParserBaseListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.util.List;

public class MemberVariableAnalyzer extends JavaParserBaseListener {
    private int targetLineNumber;

    private String variableName = "";
    private String type = "";
    private String isFinal = "";
    private String hasNew = "";
    private String output = "";

    public MemberVariableAnalyzer(int targetLineNumber) {
        this.targetLineNumber = targetLineNumber;
    }

    public String getVariableName() {
        return variableName;
    }

    public String getType() {
        return type;
    }

    public String getIsFinal() {
        return isFinal;
    }

    public String getHasNew() {
        return hasNew;
    }

    public String getOutput() {
        return output;
    }

    @Override
    public void enterMemberDeclaration(JavaParser.MemberDeclarationContext ctx) {
        if (ctx.getStart().getLine() <= targetLineNumber &&
                ctx.getStop().getLine() >= targetLineNumber &&
                ctx.getStart().getLine() == targetLineNumber) {

            JavaParser.FieldDeclarationContext fieldCtx = ctx.getChild(JavaParser.FieldDeclarationContext.class, 0);
            if (fieldCtx != null) {
                // Check if 'final' modifier exists
                isFinal = "";
                ParserRuleContext parent = ctx.getParent(); // ClassBodyDeclarationContext
                if (parent instanceof JavaParser.ClassBodyDeclarationContext) {
                    List<JavaParser.ModifierContext> modifiers = ((JavaParser.ClassBodyDeclarationContext) parent).modifier();
                    for (JavaParser.ModifierContext mod : modifiers) {
                        if (mod.classOrInterfaceModifier() != null &&
                                mod.classOrInterfaceModifier().FINAL() != null) {
                            isFinal = "FINAL";
                            break;
                        }
                    }
                }

                // Get the type
                type = "OBJECT";
                JavaParser.TypeTypeContext typeCtx = fieldCtx.typeType();
                if (typeCtx != null) {
                    if (typeCtx.primitiveType() != null) {
                        String dataType = typeCtx.primitiveType().getText().toUpperCase();
                        if (dataType.equals("BYTE") || dataType.equals("SHORT") || dataType.equals("INT") || dataType.equals("LONG") ||
                                dataType.equals("FLOAT") || dataType.equals("DOUBLE")) {
                            type = "NUMERIC";
                        } else {
                            type = dataType;
                        }
                    } else if (typeCtx.classOrInterfaceType() != null) {
                        if (typeCtx.classOrInterfaceType().typeIdentifier() != null) {
                            String dataType = typeCtx.classOrInterfaceType().typeIdentifier().getText().toUpperCase();
                            if (dataType.equals("BYTE") || dataType.equals("SHORT") || dataType.equals("INTEGER") || dataType.equals("LONG") ||
                                    dataType.equals("FLOAT") || dataType.equals("DOUBLE")) {
                                type = "NUMERIC";
                            } else if (dataType.equals("CHARACTER")) {
                                type = "CHAR";
                            } else if (dataType.equals("BOOLEAN")) {
                                type = "BOOLEAN";
                            } else if (dataType.equals("STRING") || dataType.equals("STRINGBUILDER") || dataType.equals("STRINGBUFFER") || dataType.equals("STRINGWRITER")) {
                                type = "STRING";
                            } else if (dataType.equals("COLLECTION") ||
                                    dataType.equals("LIST") || dataType.equals("VECTOR") || dataType.equals("STACK") ||
                                    dataType.equals("SET") || dataType.equals("QUEUE") || dataType.equals("DEQUE")) {
                                type = "COLLECTION";
                            } else if (dataType.equals("MAP") || dataType.equals("HASHMAP") || dataType.equals("CONCURRENTMAP") || dataType.equals("HASHTABLE")) {
                                type = "MAP";
                            }
                        }
                    }

                    if (typeCtx.getText().contains("[]")) {
                        type += "-ARRAY";
                    }
                }

                // Get variable declarator
                JavaParser.VariableDeclaratorContext varCtx = fieldCtx.variableDeclarators().variableDeclarator(0);
                if (varCtx != null) {
                    variableName = varCtx.variableDeclaratorId().getText();

                    hasNew = "";
                    JavaParser.VariableInitializerContext initCtx = varCtx.variableInitializer();
                    if (initCtx != null && initCtx.expression() != null) {
                        if (containsNew(initCtx.expression())) {
                            hasNew = "NEW";
                        }
                    }
                }
            }
        } else if (ctx.getStart().getLine() <= targetLineNumber && ctx.getStop().getLine() >= targetLineNumber && ctx.methodDeclaration() == null) {
            JavaParser.FieldDeclarationContext fieldCtx = ctx.getChild(JavaParser.FieldDeclarationContext.class, 0);
            if (fieldCtx != null) {
                // Check if 'final' modifier exists
                isFinal = "";
                ParserRuleContext parent = ctx.getParent(); // ClassBodyDeclarationContext
                if (parent instanceof JavaParser.ClassBodyDeclarationContext) {
                    List<JavaParser.ModifierContext> modifiers = ((JavaParser.ClassBodyDeclarationContext) parent).modifier();
                    for (JavaParser.ModifierContext mod : modifiers) {
                        if (mod.classOrInterfaceModifier() != null &&
                                mod.classOrInterfaceModifier().FINAL() != null) {
                            isFinal = "FINAL";
                            break;
                        }
                    }
                }

                // Get the type
                type = "OBJECT";
                JavaParser.TypeTypeContext typeCtx = fieldCtx.typeType();
                if (typeCtx != null) {
                    if (typeCtx.primitiveType() != null) {
                        String dataType = typeCtx.primitiveType().getText().toUpperCase();
                        if (dataType.equals("BYTE") || dataType.equals("SHORT") || dataType.equals("INT") || dataType.equals("LONG") ||
                                dataType.equals("FLOAT") || dataType.equals("DOUBLE")) {
                            type = "NUMERIC";
                        } else {
                            type = dataType;
                        }
                    } else if (typeCtx.classOrInterfaceType() != null) {
                        if (typeCtx.classOrInterfaceType().typeIdentifier() != null) {
                            String dataType = typeCtx.classOrInterfaceType().typeIdentifier().getText().toUpperCase();
                            if (dataType.equals("BYTE") || dataType.equals("SHORT") || dataType.equals("INTEGER") || dataType.equals("LONG") ||
                                    dataType.equals("FLOAT") || dataType.equals("DOUBLE")) {
                                type = "NUMERIC";
                            } else if (dataType.equals("CHARACTER")) {
                                type = "CHAR";
                            } else if (dataType.equals("BOOLEAN")) {
                                type = "BOOLEAN";
                            } else if (dataType.equals("STRING") || dataType.equals("STRINGBUILDER") || dataType.equals("STRINGBUFFER") || dataType.equals("STRINGWRITER")) {
                                type = "STRING";
                            } else if (dataType.equals("COLLECTION") ||
                                    dataType.equals("LIST") || dataType.equals("VECTOR") || dataType.equals("STACK") ||
                                    dataType.equals("SET") || dataType.equals("QUEUE") || dataType.equals("DEQUE")) {
                                type = "COLLECTION";
                            } else if (dataType.equals("MAP") || dataType.equals("HASHMAP") || dataType.equals("CONCURRENTMAP") || dataType.equals("HASHTABLE")) {
                                type = "MAP";
                            }
                        }
                    }

                    if (typeCtx.getText().contains("[]")) {
                        type += "-ARRAY";
                    }
                }

                // Get variable declarator
                JavaParser.VariableDeclaratorContext varCtx = fieldCtx.variableDeclarators().variableDeclarator(0);
                if (varCtx != null) {
                    variableName = varCtx.variableDeclaratorId().getText();

                    hasNew = "";
                    JavaParser.VariableInitializerContext initCtx = varCtx.variableInitializer();
                    if (initCtx != null && initCtx.expression() != null) {
                        if (containsNew(initCtx.expression())) {
                            hasNew = "NEW";
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

                // Check 'final'
                isFinal = "";
                List<JavaParser.VariableModifierContext> modifiers = ctx.variableModifier();
                for (JavaParser.VariableModifierContext mod : modifiers) {
                    if (mod.FINAL() != null) {
                        isFinal = "FINAL";
                        break;
                    }
                }

                // Determine type
                type = "OBJECT";
                JavaParser.TypeTypeContext typeCtx = ctx.typeType();
                if (typeCtx != null) {
                    if (typeCtx.primitiveType() != null) {
                        String dataType = typeCtx.primitiveType().getText().toUpperCase();
                        if (dataType.equals("BYTE") || dataType.equals("SHORT") || dataType.equals("INT") || dataType.equals("LONG") ||
                                dataType.equals("FLOAT") || dataType.equals("DOUBLE")) {
                            type = "NUMERIC";
                        } else {
                            type = dataType;
                        }
                    } else if (typeCtx.classOrInterfaceType() != null) {
                        if (typeCtx.classOrInterfaceType().typeIdentifier() != null) {
                            String dataType = typeCtx.classOrInterfaceType().typeIdentifier().getText().toUpperCase();
                            if (dataType.equals("BYTE") || dataType.equals("SHORT") || dataType.equals("INTEGER") || dataType.equals("LONG") ||
                                    dataType.equals("FLOAT") || dataType.equals("DOUBLE")) {
                                type = "NUMERIC";
                            } else if (dataType.equals("CHARACTER")) {
                                type = "CHAR";
                            } else if (dataType.equals("BOOLEAN")) {
                                type = "BOOLEAN";
                            } else if (dataType.equals("STRING") || dataType.equals("STRINGBUILDER") || dataType.equals("STRINGBUFFER") || dataType.equals("STRINGWRITER")) {
                                type = "STRING";
                            } else if (dataType.equals("COLLECTION") ||
                                    dataType.equals("LIST") || dataType.equals("VECTOR") || dataType.equals("STACK") ||
                                    dataType.equals("SET") || dataType.equals("QUEUE") || dataType.equals("DEQUE")) {
                                type = "COLLECTION";
                            } else if (dataType.equals("MAP") || dataType.equals("HASHMAP") || dataType.equals("CONCURRENTMAP") || dataType.equals("HASHTABLE")) {
                                type = "MAP";
                            }
                        }
                    }

                    if (typeCtx.getText().contains("[]")) {
                        type += "-ARRAY";
                    }
                }

                // Check each variable declarator
                for (JavaParser.VariableDeclaratorContext varDeclCtx : ctx.variableDeclarators().variableDeclarator()) {
                    variableName = varDeclCtx.variableDeclaratorId().getText();

                    hasNew = "";
                    if (varDeclCtx.variableInitializer() != null &&
                            varDeclCtx.variableInitializer().expression() != null) {
                        if (containsNew(varDeclCtx.variableInitializer().expression())) {
                            hasNew = "NEW";
                        }
                    }
                }

            }
        }else if (ctx.getStart().getLine() <= targetLineNumber && ctx.getStop().getLine() >= targetLineNumber) {

            // Check if the parent context is not a forInit
            if (!(ctx.getParent() instanceof JavaParser.ForInitContext)) {

                // Check 'final'
                isFinal = "";
                List<JavaParser.VariableModifierContext> modifiers = ctx.variableModifier();
                for (JavaParser.VariableModifierContext mod : modifiers) {
                    if (mod.FINAL() != null) {
                        isFinal = "FINAL";
                        break;
                    }
                }

                // Determine type
                type = "OBJECT";
                JavaParser.TypeTypeContext typeCtx = ctx.typeType();
                if (typeCtx != null) {
                    if (typeCtx.primitiveType() != null) {
                        String dataType = typeCtx.primitiveType().getText().toUpperCase();
                        if (dataType.equals("BYTE") || dataType.equals("SHORT") || dataType.equals("INT") || dataType.equals("LONG") ||
                                dataType.equals("FLOAT") || dataType.equals("DOUBLE")) {
                            type = "NUMERIC";
                        } else {
                            type = dataType;
                        }
                    } else if (typeCtx.classOrInterfaceType() != null) {
                        if (typeCtx.classOrInterfaceType().typeIdentifier() != null) {
                            String dataType = typeCtx.classOrInterfaceType().typeIdentifier().getText().toUpperCase();
                            if (dataType.equals("BYTE") || dataType.equals("SHORT") || dataType.equals("INTEGER") || dataType.equals("LONG") ||
                                    dataType.equals("FLOAT") || dataType.equals("DOUBLE")) {
                                type = "NUMERIC";
                            } else if (dataType.equals("CHARACTER")) {
                                type = "CHAR";
                            } else if (dataType.equals("BOOLEAN")) {
                                type = "BOOLEAN";
                            } else if (dataType.equals("STRING") || dataType.equals("STRINGBUILDER") || dataType.equals("STRINGBUFFER") || dataType.equals("STRINGWRITER")) {
                                type = "STRING";
                            } else if (dataType.equals("COLLECTION") ||
                                    dataType.equals("LIST") || dataType.equals("VECTOR") || dataType.equals("STACK") ||
                                    dataType.equals("SET") || dataType.equals("QUEUE") || dataType.equals("DEQUE")) {
                                type = "COLLECTION";
                            } else if (dataType.equals("MAP") || dataType.equals("HASHMAP") || dataType.equals("CONCURRENTMAP") || dataType.equals("HASHTABLE")) {
                                type = "MAP";
                            }
                        }
                    }

                    if (typeCtx.getText().contains("[]")) {
                        type += "-ARRAY";
                    }
                }

                // Check each variable declarator
                for (JavaParser.VariableDeclaratorContext varDeclCtx : ctx.variableDeclarators().variableDeclarator()) {
                    variableName = varDeclCtx.variableDeclaratorId().getText();

                    hasNew = "";
                    if (varDeclCtx.variableInitializer() != null &&
                            varDeclCtx.variableInitializer().expression() != null) {
                        if (containsNew(varDeclCtx.variableInitializer().expression())) {
                            hasNew = "NEW";
                        }
                    }
                }

            }

        }
    }

    private boolean containsNew(JavaParser.ExpressionContext ctx) {
        if (ctx == null) return false;
        if (ctx.NEW() != null) return true;

        for (ParseTree child : ctx.children) {
            if (child instanceof JavaParser.ExpressionContext) {
                if (containsNew((JavaParser.ExpressionContext) child)) {
                    return true;
                }
            }
        }

        return false;
    }

}