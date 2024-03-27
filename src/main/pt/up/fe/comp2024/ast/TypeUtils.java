package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisUtils;

import java.util.Optional;

public class TypeUtils {

  private static final String INT_TYPE_NAME = "int";
  private static final String BOOL_TYPE_NAME = "boolean";
  private static final String STRING_TYPE_NAME = "String";

  public static String getIntTypeName() { return INT_TYPE_NAME; }

  public static String getBoolTypeName() { return BOOL_TYPE_NAME; }
  public static String getStringTypeName() { return STRING_TYPE_NAME; }

  /**
   * Gets the {@link Type} of an arbitrary expression.
   *
   * @param expr
   * @param table
   * @return
   */
  public static Type getExprType(JmmNode expr, SymbolTable table) {
    // TODO: Simple implementation that needs to be expanded

    var kind = Kind.fromString(expr.getKind());

    Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR -> getVarExprType(expr, table);
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            case PARENTHESIS -> getExprType(expr.getChildren().get(0), table);
            case VAR_METHOD -> table.getReturnType(expr.get("name"));
            case BOOL -> new Type(BOOL_TYPE_NAME, false);
            case THIS -> new Type(table.getClassName(), false);
            case ACCESS_ARRAY -> new Type(getExprType(expr.getChildren().get(0), table).getName(), false);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "-", "/", "*"-> new Type(INT_TYPE_NAME, false);
            case "&&", "<" -> new Type(BOOL_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }

    public static Type getOperatorOperandsType(JmmNode binaryExpr) {
        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "-", "/", "*", "<" -> new Type(INT_TYPE_NAME, false);
            case "&&" -> new Type(BOOL_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }

    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
        String name = varRefExpr.get("name");

        Optional<Symbol> varRefSymbol = AnalysisUtils.validateSymbolFromSymbolTable(table, name);

        if(varRefSymbol.isEmpty()) {
            throw new RuntimeException("Undeclared variable semantic analysis pass has failed!");
        }

        return varRefSymbol.get().getType();
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        // TODO: Simple implementation that needs to be expanded
        return sourceType.getName().equals(destinationType.getName());
    }
}
