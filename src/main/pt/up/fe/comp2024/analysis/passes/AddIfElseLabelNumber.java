package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;

import java.util.List;

public class AddIfElseLabelNumber extends AnalysisVisitor  {
    private int labelNumber = 0;
    private String labelName = "ifLabel";
    @Override
    protected void buildVisitor() {
        addVisit(Kind.IF_ELSE_STMT, this::visitIfElseStmt);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
    }

    private boolean isOllirIfInducingBinaryExpr(JmmNode binaryExpr) {
        return binaryExpr.get("op").equals("&&");
    }

    private Void visitBinaryExpr(JmmNode node, SymbolTable table) {
        if(!this.isOllirIfInducingBinaryExpr(node)) return null;

        node.put(this.labelName, String.valueOf(this.labelNumber));

        this.labelNumber++;
        return null;
    }

    private Void visitIfElseStmt(JmmNode node, SymbolTable table) {
        node.put(this.labelName, String.valueOf(this.labelNumber));

        JmmNode ifScopeStmt = node.getJmmChild(1);
        JmmNode elseScopeStmt = node.getJmmChild(2);

        this.assignScopeStmtLabelToItsFirstInstruction(ifScopeStmt);
        this.assignScopeStmtLabelToItsFirstInstruction(elseScopeStmt);

        this.labelNumber++;
        return null;
    }

    private Void assignScopeStmtLabelToItsFirstInstruction(JmmNode scopeStmt) {
        if(scopeStmt.getChildren().size() < 1) return null;

        JmmNode firstInstruction = scopeStmt.getJmmChild(0);
        firstInstruction.put(this.labelName, String.valueOf(this.labelNumber));

        return null;
    }
}
