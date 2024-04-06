package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.analysis.SemanticErrorUtils;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;

public class CheckVarArg extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        List<Symbol> symbols = table.getParameters(method.get("name"));

        int varargCount = 0;
        for (Symbol symbol : symbols) {
            if (symbol.getType().getName().equals(TypeUtils.getVarargTypeName())) {
                Symbol lastSymbol = symbols.getLast();

                if ((!lastSymbol.getType().getName().equals(TypeUtils.getIntTypeName()) &&
                        !lastSymbol.getType().equals(symbol.getType())) ||
                        varargCount > 0) {
                    String message = varargCount > 0 ?
                            "Only one parameter can be vararg." :
                            SemanticErrorUtils.incompatibleType(
                                    symbol.getType().toString(),
                                    lastSymbol.getType().toString(),
                                    "Vararg");

                    addReport(Report.newError(Stage.SEMANTIC,
                            NodeUtils.getLine(method),
                            NodeUtils.getColumn(method),
                            message, null));
                    break;
                }

                varargCount++;
            }
        }

        return null;
    }
}