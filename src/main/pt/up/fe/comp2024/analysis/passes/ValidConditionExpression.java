package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.analysis.SemanticErrorUtils;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

public class ValidConditionExpression extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.IF_ELSE_STMT, this::visitIfElseStmt);
    }

    private Void visitIfElseStmt(JmmNode ifStmt, SymbolTable table) {
        JmmNode expr = ifStmt.getChild(0);

        Type exprType;

        if (!Kind.fromString(expr.getKind()).equals(Kind.BINARY_EXPR)) {
            exprType = TypeUtils.getExprType(expr, table);
        }
        else {
            exprType = TypeUtils.getBinExprFinalType(expr, null);
        }

        if (!exprType.getName().equals(TypeUtils.getBoolTypeName())) {
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(ifStmt),
                    NodeUtils.getColumn(ifStmt),
                    SemanticErrorUtils.incompatibleType(exprType.toString(), "IF statement"),
                    null));
        }

        return null;
    }
}
