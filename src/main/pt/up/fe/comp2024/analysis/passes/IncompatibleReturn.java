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
    }

    private Object visitMethodDecl(JmmNode methodDecl, SymbolTable symbolTable) {
        String methodName = methodDecl.get("name");
        Type returnType = symbolTable.getReturnType(methodName);
        List<JmmNode> returns = methodDecl.getDescendants(Kind.RETURN_STMT);

        for(JmmNode returnNode : returns) {
            if(returnNode.getChildren().isEmpty())
                continue;
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
