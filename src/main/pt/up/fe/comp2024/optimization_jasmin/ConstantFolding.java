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
        JmmNode newNode = null;

        if (left.isInstance(Kind.INTEGER_LITERAL) && right.isInstance(Kind.INTEGER_LITERAL)) {
            newNode = this.handleInteger(left, right, binaryExpr.get("op"));
        }
        else if (left.isInstance(Kind.BOOL) && right.isInstance(Kind.BOOL)) {
            newNode = this.handleBool(left, right, binaryExpr.get("op"));
        }

        if (newNode == null) return null;

        binaryExpr.replace(newNode);
        this.changed = true;

        return null;
    }

    private JmmNode handleInteger(JmmNode left, JmmNode right, String operator) {
        var val1 = Integer.parseInt(left.get("value"));
        var val2 = Integer.parseInt(right.get("value"));

        JmmNode newNode;

        if (operator.equals("<")) {
            newNode = new JmmNodeImpl(Kind.BOOL.toString());
            newNode.put("value", String.valueOf(val1 < val2));
            return newNode;
        }

        var result = switch (operator) {
            case "+" -> val1 + val2;
            case "-" -> val1 - val2;
            case "*" -> val1 * val2;
            case "/" -> val1 / val2;
            default -> throw new RuntimeException("operator '" + operator + "' not implemented");
        };

        newNode = new JmmNodeImpl(Kind.INTEGER_LITERAL.toString());
        newNode.put("value", String.valueOf(result));

        return newNode;
    }

    private JmmNode handleBool(JmmNode left, JmmNode right, String operator) {
        var val1 = Boolean.valueOf(left.get("value"));
        var val2 = Boolean.valueOf(right.get("value"));

        boolean result = switch (operator) {
            case "&&" -> val1 && val2;
            default -> throw new RuntimeException("operator '" + operator + "' not implemented");
        };

        JmmNode newNode = new JmmNodeImpl(Kind.BOOL.toString());
        newNode.put("value", String.valueOf(result));

        return newNode;
    }

    public boolean hasChanged() {
        return this.changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }
}
