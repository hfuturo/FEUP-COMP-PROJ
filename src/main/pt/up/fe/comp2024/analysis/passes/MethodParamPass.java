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
import java.util.Optional;

public class MethodParamPass extends AnalysisVisitor {
    @Override
    protected void buildVisitor() {
        this.addVisit(Kind.VAR_METHOD, this::visitVarMethod);
    }

    private Void visitVarMethod(JmmNode node, SymbolTable table) {
        List<JmmNode> children = node.getChildren();
        JmmNode caller = children.get(0);

        boolean callerIsNotThis = !caller.isInstance(Kind.THIS);
        if(callerIsNotThis ) {
            return null;
        }

        List<Symbol> params = table.getParameters(node.get("name"));

        if(params == null || params.isEmpty()) {
            return null;
        }

        for(int i = 1; i < children.size(); i++) {
            Type type = params.get(i - 1).getType();

            if(type.getName().equals(TypeUtils.getVarargTypeName())) {
                for(int j = i; j < children.size(); j++) {
                    children.get(j).put("insideMethodReturnType", type.getName());
                }

                break;
            }

            JmmNode paramNode = children.get(i);
            paramNode.put("insideMethodReturnType", type.getName());
        }

        return null;
    }
}

