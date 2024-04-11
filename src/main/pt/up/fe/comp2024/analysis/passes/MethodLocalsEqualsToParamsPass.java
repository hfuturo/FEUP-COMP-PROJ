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

import java.util.List;

public class MethodLocalsEqualsToParamsPass extends AnalysisVisitor{
    private String currentMethod;

    public MethodLocalsEqualsToParamsPass() {
        this.currentMethod = "";
    }

    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
    }

    private Void visitMethodDecl(JmmNode node, SymbolTable table) {
        this.currentMethod = node.get("name");

        return null;
    }

    private Void visitVarDecl(JmmNode node, SymbolTable table) {
        List<Symbol> params = table.getParameters(currentMethod);

        if(params != null) {
            boolean varDeclInMethodParams = params.stream().anyMatch(symbol -> {
                return symbol.getName().equals(node.get("name"));
            });

            if (varDeclInMethodParams) {
                String message = String.format("%s is already declared on %s's parameters", node.get("name"), currentMethod);
                addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(node),
                        NodeUtils.getColumn(node), message, null));
            }
        }

        return null;
    }
}
