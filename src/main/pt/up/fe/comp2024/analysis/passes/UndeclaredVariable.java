package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisUtils;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.analysis.SemanticErrorUtils;
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

  private String currentMethod;

  @Override
  public void buildVisitor() {
    addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    addVisit(Kind.INNER_MAIN_METHOD, this::visitInnerMainMethod);
    addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
  }

  private Void visitMethodDecl(JmmNode method, SymbolTable table) {
    this.currentMethod = method.get("name");
    return null;
  }

  private Void visitInnerMainMethod(JmmNode method, SymbolTable table) {
    this.currentMethod = "main";
    return null;
  }

  private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {

    SpecsCheck.checkNotNull(currentMethod,
                            () -> "Expected current method to be set");

    var varRefName = varRefExpr.get("name");

    Optional<Symbol> symbol = AnalysisUtils.validateSymbolFromSymbolTable(currentMethod, table, varRefName);
    boolean isImport = AnalysisUtils.validateIsImported(varRefName, table);

    if(symbol.isEmpty() && !isImport) {
      addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(varRefExpr),
              NodeUtils.getColumn(varRefExpr), SemanticErrorUtils.undeclaredVarMsg(varRefName), null));
    }

    if (symbol.isEmpty()) {
      if (isImport && (varRefExpr.getParent().isInstance(Kind.ASSIGN_STMT) || varRefExpr.getParent().isInstance(Kind.RETURN_STMT))) {
        addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(varRefExpr),
                NodeUtils.getColumn(varRefExpr), SemanticErrorUtils.undeclaredVarMsg(varRefName), null));
      }

      if(!isImport) {
        addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(varRefExpr),
                NodeUtils.getColumn(varRefExpr), SemanticErrorUtils.undeclaredVarMsg(varRefName), null));
      }
    }

    if(AnalysisUtils.validateIsField(varRefName, currentMethod, table)) {
      varRefExpr.put("isField", "True");
    } else {
      varRefExpr.put("isField", "False");
    }

    varRefExpr.put("assignLeft", "False");

    return null;
  }
}
