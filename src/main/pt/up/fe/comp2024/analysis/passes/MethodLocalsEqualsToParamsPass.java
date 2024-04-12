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
    public void buildVisitor() {
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
    }

    private Void visitVarDecl(JmmNode node, SymbolTable table) {
        var method = node.getAncestor(Kind.METHOD_DECL);

        if(method.isPresent()) {
            List<Symbol> params = table.getParameters(method.get().get("name"));
            String methodName = method.get().get("name");

            if (params != null) {
                boolean varDeclInMethodParams = params.stream().anyMatch(symbol -> {
                    return symbol.getName().equals(node.get("name"));
                });

                if (varDeclInMethodParams) {
                    String message = String.format("%s is already declared on %s's parameters", node.get("name"), methodName);
                    addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(node),
                            NodeUtils.getColumn(node), message, null));
                }
            }
        }

        return null;
    }
}
