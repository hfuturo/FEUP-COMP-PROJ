package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisUtils;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.Optional;

public class VarArgDeclPass extends AnalysisVisitor {
    private String currentClass;
    private String currentMethod;
    @Override
    protected void buildVisitor() {
        // things that must not be vararg -> var decl, field decl ,method returns
        // add visit to field decl and verify type
        // add visit to var decl
        addVisit(Kind.CLASS_DECL, this::visitClassDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
    }

    private Void visitClassDecl(JmmNode classDecl, SymbolTable symbolTable) {
        this.currentClass = classDecl.get("name");
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        this.currentMethod = method.get("name");

        Type type = table.getReturnType(currentMethod);

        if(type.getName().equals(TypeUtils.getVarargTypeName())) {
            var message = String.format("Return type of '%s' declared as vararg", currentMethod);
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(method),
                    NodeUtils.getColumn(method), message, null));
        }

        return null;
    }
    private Void visitVarDecl(JmmNode varDecl, SymbolTable table) {
        SpecsCheck.checkNotNull(currentClass,
                () -> "Expected current class to be set");

        var varDeclName = varDecl.get("name");

        // currentMethod may be null if varDecl is a class field
        var currentScope = currentMethod == null ? currentClass : currentMethod;

        Optional<Symbol> symbol = AnalysisUtils.validateSymbolFromSymbolTable(currentScope, table, varDeclName);

        if(symbol.isEmpty()) {
            var message = String.format("Variable '%s' does not exist.", varDeclName);
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(varDecl),
                    NodeUtils.getColumn(varDecl), message, null));
        } else {
            Symbol realSymbol = symbol.get();
            Type type = realSymbol.getType();

            if(type.getName().equals(TypeUtils.getVarargTypeName())) {
                var message = String.format("Variable '%s' declared as vararg", varDeclName);
                addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(varDecl),
                        NodeUtils.getColumn(varDecl), message, null));
            }
        }

        return null;
    }

}
