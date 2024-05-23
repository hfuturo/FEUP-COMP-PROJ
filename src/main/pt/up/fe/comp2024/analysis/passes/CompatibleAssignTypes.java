package pt.up.fe.comp2024.analysis.passes;

import org.w3c.dom.Node;
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

import java.util.List;

public class CompatibleAssignTypes extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        List<JmmNode> children = assignStmt.getChildren();
        JmmNode leftExpr = children.get(0);
        JmmNode rightExpr = children.get(1);

        leftExpr.put("assignLeft", "True");
        rightExpr.put("assignLeft", "False");

        Type leftType = TypeUtils.getExprType(leftExpr, table);
        Type rightType = TypeUtils.getExprType(rightExpr, table);

        if(!leftExpr.getKind().equals("VarRefExpr") && (
                leftExpr.isInstance(Kind.ACCESS_ARRAY) && !leftExpr.getChild(0).isInstance(Kind.VAR_REF_EXPR))
        ) {
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(leftExpr),
                    NodeUtils.getColumn(leftExpr), "Assign of non var ref", null));

            return null;
        }

        if (rightExpr.isInstance(Kind.INIT_ARRAY)) {
            if (!TypeUtils.checkValuesInArrayInit(rightExpr.getChildren(), table)) {
                String message = SemanticErrorUtils.incompatibleType(leftType.toString(), rightType.toString(), "ArrayInit");
                    addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(rightExpr),
                            NodeUtils.getColumn(rightExpr), message, null));

                    return null;
            }
        }

        if (!TypeUtils.areTypesAssignable(leftType, rightType, table)) {
            String message = SemanticErrorUtils.incompatibleType(leftType.toString(), rightType.toString(), "=");
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(assignStmt),
                    NodeUtils.getColumn(assignStmt), message, null));

        }

        return null;
    }
}
