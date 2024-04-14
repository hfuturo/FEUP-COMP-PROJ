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

import java.util.Collections;
import java.util.List;

public class CheckDuplicated extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.IMPORT, this::visitImport);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
        addVisit(Kind.PARAM, this::visitParam);
    }

    private Void visitParam(JmmNode node, SymbolTable table) {
        String methodName = node.getParent().get("name");

        List<String> params = table.getParameters(methodName).stream().map(Symbol::getName).toList();
        if (Collections.frequency(params, node.get("name")) > 1) {
            addError(node, "param", node.get("name"));
        }

        return null;
    }

    private Void visitImport(JmmNode node, SymbolTable table) {
        List<String> imports = table.getImports();

        if (Collections.frequency(imports, node.get("ID")) > 1) {
            addError(node, "import", node.get("ID"));
        }

        return null;
    }

    private Void visitMethodDecl(JmmNode node, SymbolTable table) {

        // método duplicados
        String methodName = node.get("name");
        List<String> methods = table.getMethods();

        if (Collections.frequency(methods, methodName) > 1) {
            addError(node, "method", methodName);
        }

        return null;
    }

    private Void visitVarDecl(JmmNode node, SymbolTable table) {
        var method = node.getAncestor(Kind.METHOD_DECL);
        var mainMethod = node.getAncestor(Kind.INNER_MAIN_METHOD);

        String varName = node.get("name");

        if (method.isPresent() || mainMethod.isPresent()) {
            String methodName = method.isPresent() ? method.get().get("name") : "main";

            List<String> variables = table.getLocalVariables(methodName).stream().map(Symbol::getName).toList();

            // vars iguais dentro da funcao
            if (Collections.frequency(variables, varName) > 1) {
                addError(node, "var", varName);
            }

            // var igual a parametro
            List<String> params = table.getParameters(methodName).stream().map(Symbol::getName).toList();
            if (Collections.frequency(params, varName) > 0) {
                addError(node, String.format("%s is already declared on %s's parameters", node.get("name"), methodName));
            }
        }
        else {
            List<String> fields = table.getFields().stream().map(Symbol::getName).toList();

            // fields duplicados
            if (Collections.frequency(fields, varName) > 1) {
                addError(node, "field", varName);
            }

        }

        return null;
    }

    private void addError(JmmNode node, String type, String name) {
        var message = SemanticErrorUtils.alreadyExistsError(type, name);
        addReport(Report.newError(Stage.SEMANTIC,
                NodeUtils.getLine(node),
                NodeUtils.getColumn(node),
                message, null));
    }

    private void addError(JmmNode node, String error) {
        addReport(Report.newError(Stage.SEMANTIC,
                NodeUtils.getLine(node),
                NodeUtils.getColumn(node),
                error, null));
    }
}
