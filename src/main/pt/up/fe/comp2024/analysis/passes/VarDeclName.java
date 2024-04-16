package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisUtils;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

/**
 * Checks if the type of the expression in a return statement is compatible with
 * the method return type.
 *
 * @author JBispo
 */
public class VarDeclName extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table) {

        var varDeclName = varDecl.get("name");

        if(AnalysisUtils.validateIsImported(varDeclName, table)) {
            var message = String.format("Variable name '%s' is of import", varDeclName);
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(varDecl),
                    NodeUtils.getColumn(varDecl), message, null));
            return null;
        }

        if(table.getClassName().equals(varDeclName)) {
            var message = String.format("Variable name '%s' is of class", varDeclName);
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(varDecl),
                    NodeUtils.getColumn(varDecl), message, null));
            return null;
        }

        return null;
    }
}
