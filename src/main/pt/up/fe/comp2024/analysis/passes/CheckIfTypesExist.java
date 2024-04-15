package pt.up.fe.comp2024.analysis.passes;

import org.w3c.dom.Node;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.Optional;

public class CheckIfTypesExist extends AnalysisVisitor {

    @Override
    protected void buildVisitor() {
        addVisit(Kind.ABSTRACT_DATA_TYPE, this::visitVarDecl);
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table) {
        boolean foundImport = false;
        String typeName = varDecl.get("name");

        if (typeName.equals(table.getClassName())) {
            return null;
        }

        for (String imp : table.getImports()) {
            if (imp.equals(varDecl.get("name"))) {
                foundImport = true;
            }
        }

        if (!foundImport) {
            var message = String.format("Type " + varDecl.get("name") + " does not exist.");
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(varDecl),
                    NodeUtils.getColumn(varDecl), message, null));
        }

        return null;
    }
}
