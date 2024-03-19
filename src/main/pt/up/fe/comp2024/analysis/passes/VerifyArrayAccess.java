package pt.up.fe.comp2024.analysis.passes;

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
        } else {
            var message = "Array access should be made through an array variable";
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(expr),
                    NodeUtils.getColumn(expr), message, null));
        }
    }

    private void checkArrayAccessHasIntegerIndex(JmmNode arrayAccess, SymbolTable table) {
        JmmNode index = arrayAccess.getChildren().get(1);

        if(!index.getKind().equals("IntegerLiteral")) {
            if(index.getKind().equals("VarRefExpr")) {
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
            } else {
                var message = "The index of an array should be an integer";
                addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(index),
                        NodeUtils.getColumn(index), message, null));
            }
        }
    }

    private Void visitArrayAccess(JmmNode arrayAccess, SymbolTable table) {
        this.checkArrayAccessIsOnArray(arrayAccess, table);
        this.checkArrayAccessHasIntegerIndex(arrayAccess, table);

        return null;
    }
}