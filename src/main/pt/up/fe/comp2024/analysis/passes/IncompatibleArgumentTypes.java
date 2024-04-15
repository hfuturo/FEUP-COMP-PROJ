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

import java.util.List;

public class IncompatibleArgumentTypes extends AnalysisVisitor {
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
        String isDeclared = methodCall.get("isDeclared");

        if(isDeclared.equals("False"))
            return null; // Method does not have signature

        List<Type> methodParamsTypes = symbolTable.getParameters(methodName).stream().map(Symbol::getType).toList();

        List<JmmNode> callParamsNodes = methodCall.getChildren();
        callParamsNodes.remove(0); // remove scope

        int length =  Math.min(callParamsNodes.size(), methodParamsTypes.size());
        for(int i = 0; i < length; i++) {
            Type methodParamType = methodParamsTypes.get(i);
            JmmNode paramNode = callParamsNodes.get(i);
            Type paramType = TypeUtils.getExprType(paramNode, symbolTable);

            if(!methodParamType.getName().equals(TypeUtils.getVarargTypeName())) {
                if(!methodParamType.equals(paramType)) {
                    var message = String.format("Unexpected type %s at call to %s. Expected %s", paramType.getName(), methodCall.get("name"), methodParamType.getName());
                    addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(paramNode),
                            NodeUtils.getColumn(paramNode), message, null));

                    return null;
                }
            } else {
                boolean paramIsArray = paramType.isArray();
                if(paramIsArray && !AnalysisUtils.allElementsOfArrayAreOfType(paramType, paramNode, symbolTable)) {
                    var message = String.format("Var arg elements are not of the same type. They should all be of type %s", paramType.getName());
                    addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(paramNode),
                            NodeUtils.getColumn(paramNode), message, null));
                }

                if(!paramType.getName().equals(TypeUtils.getIntTypeName())) {
                    var message = String.format("Unexpected type %s at call to %s. Expected %s", paramType.getName(), methodCall.get("name"), TypeUtils.getIntTypeName());
                    addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(paramNode),
                            NodeUtils.getColumn(paramNode), message, null));
                    return null;
                }
            }
        }

        if(callParamsNodes.size() < methodParamsTypes.size()) {
            var message = String.format("Missing parameters on call of %s. Expected %d, found %d", methodCall.get("name"), methodParamsTypes.size(), callParamsNodes.size());
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(methodCall),
                    NodeUtils.getColumn(methodCall), message, null));
            return null;
        } else if(callParamsNodes.size() > methodParamsTypes.size()) {
            if(!methodParamsTypes.get(methodParamsTypes.size()-1).getName().equals(TypeUtils.getVarargTypeName())) {
                var message = String.format("Received more parameters than expected on call of %s. Expected %d, found %d", methodCall.get("name"), methodParamsTypes.size(), callParamsNodes.size());
                addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(methodCall),
                        NodeUtils.getColumn(methodCall), message, null));
                return null;
            }

            for(int i = methodParamsTypes.size(); i < callParamsNodes.size(); i++) {
                String nodeType = TypeUtils.getExprType(callParamsNodes.get(i), symbolTable).getName();
                if(!nodeType.equals(TypeUtils.getIntTypeName())) {
                    var message = String.format("Unexpected type %s at call to %s. Expected %s", nodeType, methodCall.get("name"), TypeUtils.getIntTypeName());
                    addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(methodCall),
                            NodeUtils.getColumn(methodCall), message, null));
                    return null;
                }
            }
        }

        return null;
    }
}
