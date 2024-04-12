package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisUtils;

import java.util.List;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.METHOD_DECL;

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
            case INTEGER_LITERAL, LENGTH, ACCESS_ARRAY -> new Type(INT_TYPE_NAME, false);
            case PARENTHESIS -> getExprType(expr.getChildren().get(0), table);
            case VAR_METHOD -> {
                if (expr.get("isDeclared").equals("False"))
                    yield new Type(IMPORT_TYPE_NAME, false);

                yield table.getReturnType(expr.get("name"));
            }
            case NEW_CLASS -> getNewClassType(expr, table);
            case NEW_INT, INIT_ARRAY -> new Type(INT_TYPE_NAME, true);
            case BOOL, UNARY -> new Type(BOOL_TYPE_NAME, false);
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

         Optional<JmmNode> method = varRefExpr.getAncestor(METHOD_DECL);
         Optional<Symbol> varRefSymbol;

         if (method.isPresent()) {
             varRefSymbol = AnalysisUtils.validateSymbolFromSymbolTable(method.get().get("name"), table, name);
         }
         else {
             varRefSymbol = AnalysisUtils.validateSymbolFromSymbolTable(table, name);
         }

         if (varRefSymbol.isEmpty()) {
             boolean isImport = AnalysisUtils.validateIsImported(name, table);

             if (isImport) {
                 return new Type(TypeUtils.getImportTypeName(), false);
             }

             return null;
         }

         return varRefSymbol.get().getType();
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

        // caso seja import A, B; A = B
        for (String imp : imports) {
            if (imp.equals(destName))
                return true;
        }

        // import A; int a = A.foo()
        return destName.equals(TypeUtils.getImportTypeName());
    }

    public static boolean checkValuesInArrayInit(Type leftType, List<JmmNode> valuesNodes, SymbolTable table) {
        for (JmmNode node : valuesNodes) {
            Type nodeType = getExprType(node, table);

            if (nodeType.isArray() || !nodeType.getName().equals(leftType.getName()))
                return false;
        }
        return true;
    }

    public static Type getVarMethodType(JmmNode node, SymbolTable table) {
        System.out.println("node:\t" + node.toString());
        var expr = getVarExprType(node.getJmmChild(0), table);
        System.out.println("varType:\t" + expr.toString());
        return expr;
    }
}
