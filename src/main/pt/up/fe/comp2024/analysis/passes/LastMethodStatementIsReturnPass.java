package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;

public class LastMethodStatementIsReturnPass extends AnalysisVisitor  {
    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.INNER_MAIN_METHOD, this::visitMethodDecl);
    }

    private Object visitMethodDecl(JmmNode methodDecl, SymbolTable table) {
        List<JmmNode> expressions = methodDecl.getChildren();
        int expressionsSize = expressions.size();

        if(expressionsSize == 0) {
            return null;
        }

        String methodType = methodDecl.getKind().equals("InnerMainMethod") ? "VOID" : table.getReturnType(methodDecl.get("name")).getName();

        boolean lastStatementIsReturn = Kind.RETURN_STMT.check(expressions.get(expressionsSize - 1));
        if(!lastStatementIsReturn && !methodType.equals("VOID")) {
            var message = "Method's last statement is not a return";
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(methodDecl),
                    NodeUtils.getColumn(methodDecl), message, null));
        }

        return null;
    }

}
