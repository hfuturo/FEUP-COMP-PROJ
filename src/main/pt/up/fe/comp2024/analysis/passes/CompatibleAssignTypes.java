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
    private String methodName;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        this.methodName = method.get("name");
        return null;
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        List<JmmNode> children = assignStmt.getChildren();
        JmmNode leftVar = children.get(0);
        JmmNode rightExpr = children.get(1);

        Type leftType = TypeUtils.getTypeByString(leftVar.get("name"), table, methodName);
        Type rightType = TypeUtils.getExprType(rightExpr, table);

        System.out.println("left:\n" + leftType.toString());
        System.out.println("right:\n" + rightType.toString());

        if (rightType.isArray()) {
            if (!TypeUtils.checkValuesInArrayInit(leftType, rightExpr.getChildren(), table)) {
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
