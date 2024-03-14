package pt.up.fe.comp2024.analysis.passes;

import static pt.up.fe.comp2024.ast.Kind.VAR_DECL;

import java.util.List;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
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
    // addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
    //addVisit(Kind.BOOL, this::visitBool);
    // addVisit(Kind.INTEGER_LITERAL, this::visitIntegerLiteral);
  }

  // private Void visitMethodDecl(JmmNode method, SymbolTable table) {
  //   this.currentMethod = method.get("name");
  //   return null;
  // }

  private Void visitBinaryExpr(JmmNode method, SymbolTable table) {
    List<JmmNode> children = method.getChildren();
    JmmNode leftExpr = children.get(0);
    JmmNode rightExpr = children.get(2);

    String leftType = visit(leftExpr).toString();
    String rightType = visit(rightExpr).toString();

    /* Valid cases:
     *  1 + 1 // two integer literals
     *  pos.getX() + pos.getY() // two integer returning functions
     *  1 + pos.getX() // one integer literal and one integer returning function
     */

    if (!leftType.equals(rightType)) {
      addReport(Report.newError(Stage.SEMANTIC,
                                0 /*NodeUtils.getLine(varRefExpr)*/,
                                0
                                /*NodeUtils.getColumn(varRefExpr)*/,
                                "", null));
    }

    return null;
  }
}
