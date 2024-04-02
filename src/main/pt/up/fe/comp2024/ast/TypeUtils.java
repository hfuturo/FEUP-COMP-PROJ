package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisUtils;

import java.util.List;
import java.util.Optional;

public class TypeUtils {

  private static final String INT_TYPE_NAME = "int";
  private static final String BOOL_TYPE_NAME = "boolean";
  private static final String STRING_TYPE_NAME = "String";
  private static final String IMPORT_TYPE_NAME = "import";

  private static final String VARARG_TYPE_NAME = "vararg";

  public static String getIntTypeName() { return INT_TYPE_NAME; }
  public static String getBoolTypeName() { return BOOL_TYPE_NAME; }
  public static String getStringTypeName() { return STRING_TYPE_NAME; }
  public static String getVarargTypeName() {return VARARG_TYPE_NAME; }

    public static String getImportTypeName() { return IMPORT_TYPE_NAME; }

    public static boolean isImportType(Type type) {
      return type.getName().equals(TypeUtils.getImportTypeName());
    }

  /**
   * Gets the {@link Type} of an arbitrary expression.
   *
   * @param expr
   * @param table
   * @return
   */
  public static Type getExprType(JmmNode expr, SymbolTable table) {
    var kind = Kind.fromString(expr.getKind());

    Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR -> getVarExprType(expr, table);
            case INTEGER_LITERAL, LENGTH -> new Type(INT_TYPE_NAME, false);
            case PARENTHESIS -> getExprType(expr.getChildren().get(0), table);
            case VAR_METHOD -> {
                Type returnType = table.getReturnType(expr.get("name"));

                boolean isImported = returnType == null;
                if(isImported) {
                    yield new Type(IMPORT_TYPE_NAME, false);
                }

                yield returnType;
            }
            case ACCESS_ARRAY -> new Type(getExprType(expr.getChildren().get(0), table).getName(), false);
            case NEW_CLASS -> getNewClassType(expr, table);
            case NEW_INT, INIT_ARRAY -> new Type(INT_TYPE_NAME, true);
            case BOOL -> new Type(BOOL_TYPE_NAME, false);
            case THIS -> new Type(table.getClassName(), false);
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
            boolean isImport = AnalysisUtils.validateIsImported(name, table);

            if(isImport) {
                return new Type(TypeUtils.getImportTypeName(), false);
            }

            return null;
        }

        return varRefSymbol.get().getType();
    }

    public static Type getTypeByString(String string, SymbolTable table, String currentMethod) {
        Optional<Symbol> symbol = AnalysisUtils.validateSymbolFromSymbolTable(currentMethod, table, string);

        if (symbol.isEmpty()) {
            throw new RuntimeException("Undeclared variable semantic analysis pass has failed!");
        }

        return symbol.get().getType();
    }

    private static Type getNewClassType(JmmNode newClass, SymbolTable table) {
        String newClassName = newClass.get("name");
        List<String> imports = table.getImports();
        String currentClassName = table.getClassName();

        if (currentClassName.equals(newClassName)) {
            return new Type(newClassName, false);
        }

        for (String imp : imports) {
            if (imp.equals(newClassName)) {
                return new Type(newClassName, false);
            }
        }

        throw new RuntimeException("Undeclared variable semantic analysis pass has failed!");
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType, SymbolTable table) {
        String sourceName = sourceType.getName();
        String destName = destinationType.getName();
        List<String> imports = table.getImports();
        boolean checkArrayConsistency = (sourceType.isArray() && destinationType.isArray()) || (!sourceType.isArray() && !destinationType.isArray());

        if (sourceName.equals(destName)) {
            return checkArrayConsistency;
        }

        if (destName.equals(table.getClassName()) && sourceName.equals(table.getSuper())) {
            return checkArrayConsistency;
        }

        boolean foundSource = false;
        boolean foundDest = false;

        for (String imp : imports) {
            if (imp.equals(sourceName))
                foundSource = true;
            if (imp.equals(destName))
                foundDest = true;
        }

        return foundSource && foundDest;
    }

    public static boolean checkValuesInArrayInit(Type leftType, List<JmmNode> valuesNodes, SymbolTable table) {
        for (JmmNode node : valuesNodes) {
            Type nodeType = getExprType(node, table);

            if (nodeType.isArray() || !nodeType.getName().equals(leftType.getName()))
                return false;
        }
        return true;
    }
}
