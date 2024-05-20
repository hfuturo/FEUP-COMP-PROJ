package pt.up.fe.comp2024.optimization_jasmin;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;

public class ConstantFolding extends AnalysisVisitor {

    private boolean changed;

    public ConstantFolding() {
        this.changed = false;
    }

    @Override
    public void buildVisitor() {
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable unused) {
        JmmNode left = binaryExpr.getChild(0);
        JmmNode right = binaryExpr.getChild(1);

        if (left.isInstance(Kind.INTEGER_LITERAL) && right.isInstance(Kind.INTEGER_LITERAL)) {
            var val1 = Integer.parseInt(left.get("value"));
            var val2 = Integer.parseInt(right.get("value"));

            int result = switch (binaryExpr.get("op")) {
                case "+" -> val1 + val2;
                case "-" -> val1 - val2;
                case "*" -> val1 * val2;
                case "/" -> val1 / val2;
                default -> throw new RuntimeException("operator '" + binaryExpr.get("op") + "' not implemented");
            };

            JmmNode newNode = new JmmNodeImpl(Kind.INTEGER_LITERAL.toString());
            newNode.put("value", String.valueOf(result));
            binaryExpr.replace(newNode);
            this.changed = true;
        }

        return null;
    }

    public boolean hasChanged() {
        return this.changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }
}
