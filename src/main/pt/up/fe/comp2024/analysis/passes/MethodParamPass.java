package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;

public class MethodParamPass extends AnalysisVisitor {
    @Override
    protected void buildVisitor() {
        this.addVisit(Kind.VAR_METHOD, this::visitVarMethod);
    }

    private Void visitVarMethod(JmmNode node, SymbolTable table) {
        List<JmmNode> children = node.getChildren();
        JmmNode caller = children.get(0);

        boolean callerIsNotThis = !caller.isInstance(Kind.THIS) && !TypeUtils.getExprType(caller, table).getName().equals(table.getClassName());
        if(callerIsNotThis ) {
            return null;
        }

        List<Symbol> params = table.getParameters(node.get("name"));

        if(params == null || params.isEmpty()) {
            return null;
        }

        for(int i = 1; i < children.size(); i++) {
            Type type = params.get(i - 1).getType();

            if(type.getName().equals(TypeUtils.getVarargTypeName()) && !TypeUtils.getExprType(children.get(i), table).isArray()) {
                JmmNode init_array = new JmmNodeImpl(Kind.INIT_ARRAY.toString());
                init_array.setChildren(children.subList(i, children.size()));

                for(int j = i; j < children.size(); j++) {
                    node.removeChild(node.getNumChildren()-1);
                }

                // update parent node of children taken from deleted node
                init_array.getChildren().forEach(child -> child.setParent(init_array));
                init_array.setParent(node);
                init_array.put("op", "]");
                init_array.put("insideMethodReturnType", type.getName());
                node.add(init_array);

                break;
            }

            JmmNode paramNode = children.get(i);
            paramNode.put("insideMethodReturnType", type.getName());
        }

        return null;
    }
}

