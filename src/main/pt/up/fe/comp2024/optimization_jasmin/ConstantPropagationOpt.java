package pt.up.fe.comp2024.optimization_jasmin;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.Kind;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConstantPropagationOpt extends PreorderJmmVisitor<SymbolTable, Boolean> {

    Map<String, Integer> constants = new HashMap<>();

    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }

    private Boolean visitMethodDecl(JmmNode methodDecl, SymbolTable symbolTable) {

        List<JmmNode> children = methodDecl.getChildren();

        for(var child : children) {
            String kind = child.getKind();


            switch (Kind.valueOf(kind)) {
                case ASSIGN_STMT -> {
                    // verify if is const
                    // if is const add to constants

                    // verify if right side has const values
                    // if any is a constant, substitute by it
                }
                case VAR_METHOD -> {
                    // verify if any of parameters is const
                }
                case IF_ELSE_STMT -> {
                    // do different visit
                }
                case WHILE_STMT -> {
                    // do different visit (same one?)
                }
            }
        }

        return Boolean.FALSE;
    }

    private Boolean visitAssignStmt(JmmNode assign, SymbolTable symbolTable) {
        List<JmmNode> children = assign.getChildren();

        return Boolean.FALSE;
    }
}