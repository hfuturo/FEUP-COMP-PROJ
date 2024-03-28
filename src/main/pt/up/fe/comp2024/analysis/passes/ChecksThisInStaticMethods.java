package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.INNER_MAIN_METHOD;
import static pt.up.fe.comp2024.ast.Kind.THIS;

public class ChecksThisInStaticMethods extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.MAIN_METHOD, this::MainMethod);
    }

    private Void MainMethod(JmmNode main, SymbolTable table) {
        List<JmmNode> list = main.getDescendants(THIS);

        if (list.size() > 0) {
            String message = "You can not use 'this' in static methods.";
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(list.get(0)),
                    NodeUtils.getColumn(list.get(0)), message, null));
        }

        return null;
    }
}
