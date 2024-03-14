package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisUtils;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.Optional;

/**
 * Checks if the type of the expression in a return statement is compatible with
 * the method return type.
 *
 * @author JBispo
 */
public class UndeclaredVariable extends AnalysisVisitor {

  // Ainda falta ver dos return types

  private String currentMethod;

  @Override
  public void buildVisitor() {
    addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
  }

  private Void visitMethodDecl(JmmNode method, SymbolTable table) {
    this.currentMethod = method.get("name");
    return null;
  }

  private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
    SpecsCheck.checkNotNull(currentMethod,
                            () -> "Expected current method to be set");

    var varRefName = varRefExpr.get("name");

    Optional<Symbol> symbol = AnalysisUtils.validateSymbolFromSymbolTable(currentMethod, table, varRefName);

    if(symbol.isEmpty()) {
      var message = String.format("Variable '%s' does not exist.", varRefName);
      addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(varRefExpr),
              NodeUtils.getColumn(varRefExpr), message, null));
    }

    return null;
  }
}
