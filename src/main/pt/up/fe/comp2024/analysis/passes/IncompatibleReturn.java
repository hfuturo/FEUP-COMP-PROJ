package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;

public class IncompatibleReturn extends AnalysisVisitor {
    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.INNER_MAIN_METHOD, this::visitInnerMainMethod);
    }

    private Object visitInnerMainMethod(JmmNode methodDecl, SymbolTable symbolTable) {
        List<JmmNode> returns = methodDecl.getDescendants(Kind.RETURN_STMT);

        if(returns.size() > 1) {
            var message = "Multiple returns present on method main";
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(methodDecl),
                    NodeUtils.getColumn(methodDecl), message, null));
        }

        for(JmmNode returnNode: returns) {
            if(returnNode.getChildren().isEmpty()) continue;

            JmmNode returnTypeChild = returnNode.getChild(0);
            Type returnType = TypeUtils.getExprType(returnTypeChild, symbolTable);

            if(!returnType.getName().equals("VOID")) {
                var message = String.format("Method main expected a return of type void, but found return of type %s", returnType.getName());
                addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(methodDecl),
                        NodeUtils.getColumn(methodDecl), message, null));
            }
        }

        return null;
    }

    private Object visitMethodDecl(JmmNode methodDecl, SymbolTable symbolTable) {
        String methodName = methodDecl.get("name");
        Type returnType = symbolTable.getReturnType(methodName);
        List<JmmNode> returns = methodDecl.getDescendants(Kind.RETURN_STMT);

        if(returns.size() == 0 && !returnType.getName().equals("VOID")) {
            var message = String.format("Method %s expected a return of type %s, but no return was found", methodName, returnType.getName());
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(methodDecl),
                    NodeUtils.getColumn(methodDecl), message, null));
        }

        if(returns.size() > 1) {
            var message = String.format("Found more than one return on method %s", methodName);
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(methodDecl),
                    NodeUtils.getColumn(methodDecl), message, null));
        }

        for(JmmNode returnNode : returns) {
            if(returnNode.getChildren().isEmpty()) {
                if(!returnType.getName().equals("VOID")) {
                    var message = String.format("Incompatible return in function %s. Expected %s, found void", methodName, returnType.getName());
                    addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(returnNode),
                            NodeUtils.getColumn(returnNode), message, null));
                }
                continue;
            }


            JmmNode child = returnNode.getChild(0);
            Type childType = TypeUtils.getExprType(child, symbolTable);

            if(childType == null) continue; // var does not exist
            if(!childType.getName().equals("import") && !TypeUtils.areTypesAssignable(childType, returnType, symbolTable)) {
                var message = String.format("Incompatible return in function %s. Expected %s, found %s", methodName, returnType.getName(), childType.getName());
                addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(returnNode),
                        NodeUtils.getColumn(returnNode), message, null));
            }
        }
        return null;
    }


}
