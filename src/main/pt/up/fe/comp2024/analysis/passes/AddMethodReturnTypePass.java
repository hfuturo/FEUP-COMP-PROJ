package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisUtils;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;

import java.util.Optional;

public class AddMethodReturnTypePass extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.VAR_METHOD, this::visitVarMethod);
    }

    private Void visitVarMethod(JmmNode node, SymbolTable table) {
        // meter o tipo de retorno
        Optional<Symbol> symbol = AnalysisUtils.validateSymbolFromSymbolTable(table, node.get("name"));

        symbol.ifPresent(value -> node.put("returnType", value.getType().toString()));
        return null;
    }
}
