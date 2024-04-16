package pt.up.fe.comp2024.analysis.passes;

import java.util.List;
import java.util.Optional;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisUtils;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Checks if the type of the expression in a return statement is compatible with
 * the method return type.
 *
 * @author JBispo
 */
public class VerifyArrayAccess extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ACCESS_ARRAY, this::visitArrayAccess);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        this.currentMethod = method.get("name");
        return null;
    }

    private void checkArrayAccessIsOnArray(JmmNode arrayAccess, SymbolTable table) {
        JmmNode expr = arrayAccess.getChildren().get(0);

        // se for init_array aceita imediatamente
        if (expr.isInstance(INIT_ARRAY))
            return;

        String exprKind = expr.getKind();

        String varName = expr.get("name");

        if (exprKind.equals("VarRefExpr")) {
            Optional<Symbol> symbol = AnalysisUtils.validateSymbolFromSymbolTable(currentMethod, table, varName);

            if(symbol.isEmpty()) {
                var message = String.format("Variable %s does not exist", varName);
                addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(expr),
                        NodeUtils.getColumn(expr), message, null));
            } else {
                Symbol realSymbol = symbol.get();
                if(!realSymbol.getType().isArray()) {
                    var message = String.format("Variable %s is not an array", varName);
                    addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(expr),
                            NodeUtils.getColumn(expr), message, null));
                }
            }
        }
        else if (exprKind.equals(VAR_METHOD.toString())) {
            var caller = expr.getJmmChild(0);

            if (!caller.isInstance(THIS)) {
                String callerName = caller.get("name");

                if (AnalysisUtils.validateIsImported(callerName, table))
                    return;
            }

            for (String method : table.getMethods()) {
                if (expr.get("name").equals(method)) {
                    var methodReturnType = table.getReturnType(expr.get("name")).getName();
                    if (!methodReturnType.equals(TypeUtils.getIntTypeName())) {
                        var message = "Method does not return an integer.";
                        addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(expr),
                                NodeUtils.getColumn(expr), message, null));
                    }
                    return;
                }
            }
        }
        else {
            var message = "Array access should be made through an array variable";
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(expr),
                    NodeUtils.getColumn(expr), message, null));
        }
    }

    private void checkArrayAccessHasIntegerIndex(JmmNode arrayAccess, SymbolTable table) {
        JmmNode index = arrayAccess.getChildren().get(1);

        // se for inteiro, length ou acesso a um array aceita automaticamente
        if (index.isInstance(INTEGER_LITERAL) || index.isInstance(LENGTH) || index.isInstance(ACCESS_ARRAY))
            return;

        // se for binary expression verifica se retorno é um inteiro ou bool
        if (index.isInstance(BINARY_EXPR)) {
            if (!TypeUtils.getExprType(index, table).getName().equals(TypeUtils.getIntTypeName())) {
                var message = String.format("Expression does not return an integer.");
                addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(index),
                        NodeUtils.getColumn(index), message, null));
            }
            return;
        }

        // se for uma variável verifica se é um inteiro
        if (index.isInstance(VAR_REF_EXPR)) {
            String varName = index.get("name");
            Optional<Symbol> indexSymbol = AnalysisUtils.validateSymbolFromSymbolTable(currentMethod, table, varName);

            if(indexSymbol.isEmpty()) {
                var message = String.format("Variable %s does not exist", varName);
                addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(index),
                        NodeUtils.getColumn(index), message, null));
            } else {
                Symbol symbol = indexSymbol.get();

                boolean isTypeInt = symbol.getType().getName().equals(TypeUtils.getIntTypeName());
                if(!isTypeInt) {
                    var message = "Array indexes need to be integers";
                    addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(index),
                            NodeUtils.getColumn(index), message, null));
                }
            }
            return;
        }

        // chamadas de métodos
        if (index.isInstance(VAR_METHOD)) {
            var caller = index.getJmmChild(0);


            if (!caller.isInstance(THIS)) {
                String callerName = caller.get("name");

                // se for import dá return pois aceita automaticamente.
                // se não for necessário veriricar se é a classe atual e trata como
                // se fosse um 'this'
                if (AnalysisUtils.validateIsImported(callerName, table))
                    return;
            }

            // this. ...
            for (String method : table.getMethods()) {
                if (index.get("name").equals(method)) {
                    var methodReturnType = table.getReturnType(index.get("name")).getName();
                    if (!methodReturnType.equals(TypeUtils.getIntTypeName())) {
                        var message = "Method does not return an integer.";
                        addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(index),
                                NodeUtils.getColumn(index), message, null));
                    }
                    return;
                }
            }
        }

        var message = "Array indexes need to be integers";
        addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(index),
                NodeUtils.getColumn(index), message, null));
    }

    private Void visitArrayAccess(JmmNode arrayAccess, SymbolTable table) {
        this.checkArrayAccessIsOnArray(arrayAccess, table);
        this.checkArrayAccessHasIntegerIndex(arrayAccess, table);

        return null;
    }
}