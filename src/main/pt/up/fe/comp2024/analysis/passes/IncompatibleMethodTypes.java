package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisUtils;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;

import java.util.List;
import java.util.Optional;

public class IncompatibleMethodTypes extends AnalysisVisitor {
    private String currentMethod;

    @Override
    protected void buildVisitor() {
        // types of arguments on method call must be compatible with method decl
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_METHOD, this::visitMethodCall);
    }

    private Void visitMethodDecl(JmmNode methodDecl, SymbolTable symbolTable) {
        this.currentMethod = methodDecl.get("name");
        return null;
    }

    private Void visitMethodCall(JmmNode methodCall, SymbolTable symbolTable) {
        String methodName = methodCall.get("name");
        List<JmmNode> methodExpr = methodCall.getChildren();
        JmmNode methodClass = methodExpr.remove(0);

        if(methodClass.getKind().equals("VarRefExpr")) {
            String varName = methodClass.get("name");
            Optional<Symbol> optSymbol = AnalysisUtils.validateSymbolFromSymbolTable(symbolTable, varName);
            if(optSymbol.isEmpty())
                return null;
            String type = optSymbol.get().getType().getName();
            if(!type.equals(symbolTable.getClassName()))
                return null;
        }

        List<Symbol> methodParams = symbolTable.getParameters(methodName);

        return null;
    }
}
