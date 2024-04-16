package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisUtils;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.Optional;

public class SuperIsImportedPass extends AnalysisVisitor {
    @Override
    protected void buildVisitor() {
        this.addVisit(Kind.CLASS_DECL, this::visitClassDecl);
    }

    private Void visitClassDecl(JmmNode node, SymbolTable table) {
        Optional<String> ultraSuper = node.getOptional("ultraSuper");

        if(ultraSuper.isEmpty()) {
            return null;
        }

        String superClass = ultraSuper.get();

        if(!AnalysisUtils.validateIsImported(superClass, table)) {
            var message = String.format("Super class %s is not imported.", superClass);
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(node),
                    NodeUtils.getColumn(node), message, null));
        }

        return null;
    }
}
