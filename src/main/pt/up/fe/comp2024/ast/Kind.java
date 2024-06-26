package pt.up.fe.comp2024.ast;

import java.util.Arrays;
import java.util.Set;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.specs.util.SpecsStrings;

public enum Kind {
  PROGRAM,
  IMPORT,
  BOOL,
  ACCESS_ARRAY,
  STRING_TYPE,
  CLASS_DECL,
  VAR_ARG_TYPE,
  VAR_DECL,
  TYPE,
  METHOD_DECL,
  MAIN_METHOD,
  INNER_MAIN_METHOD,
  PARAM,
  BINARY_EXPR,
  ASSIGN_STMT,
  RETURN_STMT,
  INTEGER_LITERAL,
  PARENTHESIS,
  LENGTH,
  THIS,
  NEW_CLASS,
  NEW_INT,
  IF_ELSE_STMT,
  WHILE_STMT,
  UNARY,
  INIT_ARRAY,
  VAR_REF_EXPR,
  VAR_METHOD,
  BOOL_TYPE,
  INTEGER_TYPE,
  ARRAY_TYPE,
  SCOPE_STMT,
  EXPR_STMT,
  ABSTRACT_DATA_TYPE;

  private static final Set<Kind> STATEMENTS = Set.of(ASSIGN_STMT, RETURN_STMT);
  private static final Set<Kind> EXPRESSIONS =
      Set.of(BINARY_EXPR, INTEGER_LITERAL, VAR_REF_EXPR);

  private static final Set<Kind> TYPES = Set.of(TYPE, INTEGER_TYPE, BOOL_TYPE, VAR_ARG_TYPE, ABSTRACT_DATA_TYPE, ARRAY_TYPE);

  private final String name;

  private Kind(String name) { this.name = name; }

  private Kind() { this.name = SpecsStrings.toCamelCase(name(), "_", true); }

  public static Kind fromString(String kind) {
    for (Kind k : Kind.values()) {
      if (k.getNodeName().equals(kind)) {
        return k;
      }
    }
    throw new RuntimeException("Could not convert string '" + kind +
                               "' to a Kind");
  }

  public String getNodeName() { return name; }

  @Override
  public String toString() {
    return getNodeName();
  }

  /**
   * @return true if this kind represents a statement, false otherwise
   */
  public boolean isStmt() { return STATEMENTS.contains(this); }

  /**
   * @return true if this kind represents an expression, false otherwise
   */
  public boolean isExpr() { return EXPRESSIONS.contains(this); }

  public boolean isType() { return TYPES.contains(this);}

  /**
   * Tests if the given JmmNode has the same kind as this type.
   *
   * @param node
   * @return
   */
  public boolean check(JmmNode node) {
    return node.isInstance(this);
  }

  public boolean checkIsType(JmmNode node) {
    return TYPES.contains(this);
  }

  public void checkIsTypeOrThrow(JmmNode node) {
    if(!this.checkIsType(node)) {
      throw new RuntimeException("GraphNode '" + node + "' is not a type");
    }
  }

  /**
   * Performs a check and throws if the test fails. Otherwise, does nothing.
   *
   * @param node
   */
  public void checkOrThrow(JmmNode node) {

    if (!check(node)) {
      throw new RuntimeException("GraphNode '" + node + "' is not a '" +
                                 getNodeName() + "'");
    }
  }

  /**
   * Performs a check on all kinds to test and returns false if none matches.
   * Otherwise, returns true.
   *
   * @param node
   * @param kindsToTest
   * @return
   */
  public static boolean check(JmmNode node, Kind... kindsToTest) {

    for (Kind k : kindsToTest) {

      // if any matches, return successfully
      if (k.check(node)) {

        return true;
      }
    }

    return false;
  }

  /**
   * Performs a check an all kinds to test and throws if none matches.
   * Otherwise, does nothing.
   *
   * @param node
   * @param kindsToTest
   */
  public static void checkOrThrow(JmmNode node, Kind... kindsToTest) {
    if (!check(node, kindsToTest)) {
      // throw if none matches
      throw new RuntimeException("GraphNode '" + node + "' is not any of " +
                                 Arrays.asList(kindsToTest));
    }
  }
}
