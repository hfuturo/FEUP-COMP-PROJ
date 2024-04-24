package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;

public class AddIfElseLabelNumber extends AnalysisVisitor  {
    private int labelNumber = 0;
    @Override
    protected void buildVisitor() {
        addVisit(Kind.IF_ELSE_STMT, this::visitIfElseStmt);
    }

    private Void visitIfElseStmt(JmmNode node, SymbolTable table) {
        node.put("ifLabel", String.valueOf(this.labelNumber));
        this.labelNumber += 1;

        return null;
    }
}
