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
  }

  private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
    List<JmmNode> children = binaryExpr.getChildren();
    JmmNode leftExpr = children.get(0);
    JmmNode rightExpr = children.get(1);

    Type operationType = TypeUtils.getExprType(binaryExpr, table);

    Type leftType = TypeUtils.getExprType(leftExpr, table);
    Type rightType = TypeUtils.getExprType(rightExpr, table);

    /* Valid cases:
     *  1 + 1 // two integer literals
     *  pos.getX() + pos.getY() // two integer returning functions
     *  1 + pos.getX() // one integer literal and one integer returning function
     */

    boolean leftTypeIncompatibleWithOpType = !leftType.equals(operationType);
    boolean rightTypeIncompatibleWithOpType = !rightType.equals(operationType);
    if (leftTypeIncompatibleWithOpType || rightTypeIncompatibleWithOpType) {
      var message = String.format("Incompatible types (%s, %s) in operation: %s", leftType.toString(), rightType.toString(), binaryExpr.get("op"));
      addReport(Report.newError(Stage.SEMANTIC,
                                0 /*NodeUtils.getLine(varRefExpr)*/,
                                0
                                /*NodeUtils.getColumn(varRefExpr)*/,
                                message, null));
    }

    return null;
  }
}
