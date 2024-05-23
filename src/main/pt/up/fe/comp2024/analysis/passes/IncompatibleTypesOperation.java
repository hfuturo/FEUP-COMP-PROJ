package pt.up.fe.comp2024.analysis.passes;

import java.util.List;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.analysis.SemanticErrorUtils;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Checks if the type of the expression in a return statement is compatible with
 * the method return type.
 *
 * @author JBispo
 */
public class IncompatibleTypesOperation extends AnalysisVisitor {

  @Override
  public void buildVisitor() {
    addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
    addVisit(Kind.INTEGER_LITERAL, this::visitDefault);
    addVisit(Kind.VAR_REF_EXPR, this::visitDefault);
    addVisit(Kind.THIS, this::visitDefault);
    addVisit(Kind.VAR_METHOD, this::visitDefault);
    addVisit(Kind.ACCESS_ARRAY, this::visitDefault);
    addVisit(Kind.LENGTH, this::visitDefault);
    addVisit(Kind.BOOL, this::visitDefault);
    addVisit(Kind.LENGTH, this::visitLength);
    addVisit(Kind.UNARY, this::visitUnary);
  }

  public Void visitUnary(JmmNode node, SymbolTable table) {
    JmmNode child = node.getJmmChild(0);

    if (child.isInstance(PARENTHESIS)) {
      visit(child.getChild(0), table);
    }


    Type childType = TypeUtils.getExprType(child, table);
    if (!childType.getName().equals(TypeUtils.getBoolTypeName())) {
      addReport(Report.newError(Stage.SEMANTIC,
              NodeUtils.getLine(node),
              NodeUtils.getColumn(node),
              SemanticErrorUtils.incompatibleType(childType.getName(), "!"), null));
    }

    return null;
  }

  public Type visitLength(JmmNode node, SymbolTable table) {
    JmmNode leftExprNode = node.getChildren().get(0);

    String leftExprNodeKind = leftExprNode.getKind();
    boolean exprNodeHasValidKind = leftExprNodeKind.equals(Kind.VAR_METHOD.toString())
            || leftExprNodeKind.equals(Kind.VAR_REF_EXPR.toString()) || leftExprNodeKind.equals(Kind.THIS.toString())
            || leftExprNodeKind.equals(Kind.INIT_ARRAY.toString());

    if (leftExprNode.isInstance(PARENTHESIS)) {
      visit(leftExprNode.getJmmChild(0));
    }

    if(!exprNodeHasValidKind) {
      throw new RuntimeException(String.format("GraphNode where .length was called is of invalid kind: %s", leftExprNodeKind));
    }

    Type leftExprType = TypeUtils.getExprType(leftExprNode, table);

    if(!leftExprType.isArray() && !leftExprType.getName().equals(TypeUtils.getImportTypeName())) {
      addReport(Report.newError(Stage.SEMANTIC,
              NodeUtils.getLine(leftExprNode),
              NodeUtils.getColumn(leftExprNode),
              SemanticErrorUtils.lengthOnSomethingNotArray("something"), null));
    }

    return leftExprType;
  }

  private Type visitDefault(JmmNode node, SymbolTable table) {
    return TypeUtils.getExprType(node, table);
  }

  private Type visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
    List<JmmNode> children = binaryExpr.getChildren();
    JmmNode leftExpr = children.get(0);
    JmmNode rightExpr = children.get(1);

    Type operationType = TypeUtils.getOperatorOperandsType(binaryExpr);

    Type leftType = TypeUtils.getExprType(leftExpr, table);
    Type rightType = TypeUtils.getExprType(rightExpr, table);

    if (Kind.fromString(leftExpr.getKind()).equals(PARENTHESIS)) {
      for (JmmNode descendent : leftExpr.getDescendants()) {
        if (!descendent.isInstance(PARENTHESIS)) {
          leftType = TypeUtils.getExprType(descendent ,table);
          break;
        }
      }
    }

    if (Kind.fromString(rightExpr.getKind()).equals(PARENTHESIS)) {
      for (JmmNode descendent : rightExpr.getDescendants()) {
        if (!descendent.isInstance(PARENTHESIS)) {
          rightType = TypeUtils.getExprType(descendent ,table);
          break;
        }
      }
    }

    boolean neitherTypeIsImport = !(TypeUtils.isImportType(leftType) || TypeUtils.isImportType(rightType));
    if(neitherTypeIsImport) {
      boolean leftTypeIncompatibleWithOpType = !leftType.equals(operationType);
      boolean rightTypeIncompatibleWithOpType = !rightType.equals(operationType);
      if (leftTypeIncompatibleWithOpType || rightTypeIncompatibleWithOpType) {
        var message = String.format("Incompatible types (%s, %s) in operation: %s", leftType.toString(), rightType.toString(), binaryExpr.get("op"));
        addReport(Report.newError(Stage.SEMANTIC,
                NodeUtils.getLine(leftExpr),
                NodeUtils.getColumn(leftExpr),
                message, null));
      }
    }

    return TypeUtils.getExprType(binaryExpr, table);
  }
}
