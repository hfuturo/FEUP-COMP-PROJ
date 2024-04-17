package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisUtils;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;
import java.util.Optional;

public class UndeclaredMethod extends AnalysisVisitor {
    private String currentMethod;
    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.INNER_MAIN_METHOD, this::visitMainMethod);
        addVisit(Kind.VAR_METHOD, this::visitMethodCall);
    }

    private Void visitMainMethod(JmmNode method, SymbolTable table) {
        this.currentMethod = "main";
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        this.currentMethod = method.get("name");
        return null;
    }
    private Void visitMethodCall(JmmNode methodCall, SymbolTable symbolTable) {
        String methodName = methodCall.get("name");

        methodCall.put("isDeclared", "False");
        JmmNode methodClass = methodCall.getChild(0);

        if(methodClass.getKind().equals(Kind.PARENTHESIS.toString())) {
            List<JmmNode> parenthesisChildren = methodClass.getChildren();

            for(JmmNode child: parenthesisChildren) {
                if(!child.getKind().equals(Kind.PARENTHESIS.toString())) {
                    methodClass = child;
                    break;
                } else {
                    JmmNode currChild = child;
                    while(currChild.getKind().equals(Kind.PARENTHESIS.toString())) {
                        if(currChild.getChildren().isEmpty()) break;
                        currChild = currChild.getChild(0);
                    }

                    if(!currChild.getKind().equals(Kind.PARENTHESIS.toString())) {
                        methodClass = currChild;
                        break;
                    }
                }
            }
        }

        // Isto está um esparguete enorme, mas não fui eu que fiz e não há tempo para resolver
        if(methodClass.getKind().equals("VarRefExpr")) {
            String varName = methodClass.get("name");
            Optional<Symbol> optSymbol = AnalysisUtils.validateSymbolFromSymbolTable(currentMethod, symbolTable, varName);
            if(optSymbol.isEmpty()) {
                // var does not exist
                return null;
            }

            String type = optSymbol.get().getType().getName();

            if(type.equals(symbolTable.getClassName())) {
                boolean isDeclared = symbolTable.getMethods().contains(methodName);
                if(isDeclared) methodCall.put("isDeclared", "True");
                if(isDeclared || !symbolTable.getSuper().isEmpty()) {
                    return null; // its either a function of this class or assumed is a function of super
                }
            } else if(symbolTable.getImports().contains(type)) {
                return null; // assumed as a function of imported class
            }
        } else if(methodClass.getKind().equals("This")) {
            boolean isDeclared = symbolTable.getMethods().contains(methodName);
            if(isDeclared) methodCall.put("isDeclared", "True");
            if(isDeclared || !symbolTable.getSuper().isEmpty()) {
                return null; // its either a function of this class or assumed is a function of super
            }
        } else if(methodClass.getKind().equals(Kind.NEW_CLASS.toString())) {
            String callerClassName = methodClass.get("name");
            boolean classNameIsMainClass = symbolTable.getClassName().equals(callerClassName);
            if(classNameIsMainClass) {
                boolean methodExistsInCallerClass = symbolTable.getMethods().stream().anyMatch((symbolTableRegisteredMethod) ->
                    symbolTableRegisteredMethod.equals(methodName)
                );

                if(!methodExistsInCallerClass) {
                    var message = String.format("Method %s is not declared", methodName);
                    addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(methodCall),
                            NodeUtils.getColumn(methodCall), message, null));
                }

                return null;
            } else {
                boolean isCallerClassImported = AnalysisUtils.validateIsImported(callerClassName, symbolTable);
                if(!isCallerClassImported) {
                    var message = String.format("Method %s is not declared", methodName);
                    addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(methodCall),
                            NodeUtils.getColumn(methodCall), message, null));
                }

                return null;
            }
        }
        else if(methodClass.isInstance(Kind.VAR_METHOD)) {
            Type ret = verifyMethodChainReturnType(methodCall, symbolTable);
            if (ret == null) {
                var message = "Method in chain returns wrong type / does not exist.";
                addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(methodCall),
                        NodeUtils.getColumn(methodCall), message, null));
            }
            return null;
        }

        var message = String.format("Method %s is not declared", methodName);
        addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(methodCall),
                NodeUtils.getColumn(methodCall), message, null));

        return null;
    }

    private Type verifyMethodChainReturnType(JmmNode node, SymbolTable table) {
        JmmNode caller = node.getJmmChild(0);

        if (caller.isInstance(Kind.VAR_METHOD)) {
            Type retType = verifyMethodChainReturnType(caller, table);

            // se for null, significa que houve uma invocação inválida
            if (retType == null)
                return null;

            // se tipo retornado é da classe atual, verifica se método que node invoca existe
            if (retType.getName().equals(table.getClassName())) {
                Optional<Type> ret = table.getReturnTypeTry(node.get("name"));

                if (ret.isEmpty()) {
                    if (!table.getSuper().trim().equals("")) {
                        return new Type(table.getClassName(), false);
                    }
                    System.out.println("entra");
                    return  null;
                }

                return ret.get();
            }
            return null;
        }
        // chegou ao inicio da chain
        else {
            Type callerType = TypeUtils.getExprType(caller, table);
            // se caller for THIS ou elemento da classe atual, verifica se método exite e retorna type do método
            if (caller.isInstance(Kind.THIS) || callerType.getName().equals(table.getClassName())) {
                Optional<Type> returnTypeTry = table.getReturnTypeTry(node.get("name"));

                if (returnTypeTry.isEmpty()) {
                    if (!table.getSuper().trim().equals("")) {
                        return new Type(table.getClassName(), false);
                    }
                    return null;
                }

                return returnTypeTry.get();
            }
        }
        return null;
    }
}
