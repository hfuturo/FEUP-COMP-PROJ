package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisUtils;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.Optional;

public class UndeclaredMethod extends AnalysisVisitor {
    private String currentMethod;
    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_METHOD, this::visitMethodCall);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        this.currentMethod = method.get("name");
        return null;
    }
    private Void visitMethodCall(JmmNode methodCall, SymbolTable symbolTable) {
        String methodName = methodCall.get("name");
        JmmNode methodClass = methodCall.getChild(0);

        if(methodClass.getKind().equals("VarRefExpr")) {
            String varName = methodClass.get("name");
            // TODO maybe should create function to check if is a class import
            Optional<Symbol> optSymbol = AnalysisUtils.validateSymbolFromSymbolTable(currentMethod, symbolTable, varName);

            if(optSymbol.isEmpty()) {
                var message = String.format("Variable %s is not declared", varName);
                addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(methodClass),
                        NodeUtils.getColumn(methodClass), message, null));

                return null;
            }

            String type = optSymbol.get().getType().getName();
            if(type.equals(symbolTable.getClassName())) {
                if(symbolTable.getMethods().contains(methodName) || !symbolTable.getSuper().isEmpty()) {
                    return null; // its either a function of this class or assumed is a function of super
                }
            } else if(symbolTable.getImports().contains(type)) {
                    return null; // assumed as a function of imported class
            }
        } else if(methodClass.getKind().equals("This")) {
            if(symbolTable.getMethods().contains(methodName) || !symbolTable.getSuper().isEmpty()) {
                return null; // its either a function of this class or assumed is a function of super
            }
        }

        var message = String.format("Method %s is not declared", methodName);
        addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(methodCall),
                NodeUtils.getColumn(methodCall), message, null));

        return null;
    }
}
