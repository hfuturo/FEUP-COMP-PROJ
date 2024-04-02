package pt.up.fe.comp2024.analysis.passes;

import static pt.up.fe.comp2024.ast.Kind.VAR_DECL;

import java.util.List;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

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
  }

  private Type visitDefault(JmmNode node, SymbolTable table) {
    Type type = TypeUtils.getExprType(node, table);
    return type;
  }

  private Type visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
    List<JmmNode> children = binaryExpr.getChildren();
    JmmNode leftExpr = children.get(0);
    JmmNode rightExpr = children.get(1);

    Type operationType = TypeUtils.getOperatorOperandsType(binaryExpr);

    Type leftType = (Type) visit(leftExpr, table);
    Type rightType = (Type) visit(rightExpr, table);

    boolean neitherTypeIsImport = !(TypeUtils.isImportType(leftType) || TypeUtils.isImportType(rightType));
    if(neitherTypeIsImport) {
      boolean leftTypeIncompatibleWithOpType = !leftType.equals(operationType);
      boolean rightTypeIncompatibleWithOpType = !rightType.equals(operationType);
      if (leftTypeIncompatibleWithOpType || rightTypeIncompatibleWithOpType) {
        var message = String.format("Incompatible types (%s, %s) in operation: %s", leftType.toString(), rightType.toString(), binaryExpr.get("op"));
        addReport(Report.newError(Stage.SEMANTIC,
                NodeUtils.getLine(binaryExpr),
                NodeUtils.getColumn(binaryExpr),
                message, null));
      }
    }

    return TypeUtils.getExprType(binaryExpr, table);
  }
}
