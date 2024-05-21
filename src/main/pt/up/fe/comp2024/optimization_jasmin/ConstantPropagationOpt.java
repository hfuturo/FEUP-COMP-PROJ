package pt.up.fe.comp2024.optimization_jasmin;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2024.CompilerConfig;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.parser.JmmParserImpl;

import java.util.*;

public class ConstantPropagationOpt extends PreorderJmmVisitor<SymbolTable, Boolean> {

    Map<String, String> constants = new HashMap<>();

    boolean changed;
    JmmParserImpl parser = new JmmParserImpl();

    public ConstantPropagationOpt() {
        setDefaultValue(() -> null);
        this.changed = false;
    }

    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.INNER_MAIN_METHOD, this::visitMainMethod);
    }

    private Boolean visitMethodDecl(JmmNode methodDecl, SymbolTable symbolTable) {
        constants.clear();
        List<JmmNode> children = methodDecl.getChildren();

        return visitNodeChildren(children, this.constants);
    }

    private Boolean visitMainMethod(JmmNode methodDecl, SymbolTable symbolTable) {
        constants.clear();
        List<JmmNode> children = methodDecl.getChildren();

        return visitNodeChildren(children, this.constants);
    }

    private Boolean visitAssignStmt(JmmNode assign, Map<String, String> constants) {
        Boolean returnValue = Boolean.FALSE;
        List<JmmNode> children = assign.getChildren();
        JmmNode variable = children.get(0);
        String variableName = variable.hasAttribute("name") ? variable.get("name") : "_INVALID";

        if(Kind.fromString(variable.getKind()).equals(Kind.ACCESS_ARRAY)) {
            returnValue |= visitArrayAcces(variable, constants);
        }

        JmmNode rightHandSide = children.get(1);

        if(getKind(rightHandSide).equals(Kind.INTEGER_LITERAL) && getKind(variable).equals(Kind.VAR_REF_EXPR)) {
            String number = rightHandSide.get("value");
            constants.put(variableName, number);
        } else if(Kind.fromString(rightHandSide.getKind()).equals(Kind.VAR_REF_EXPR)) {
            returnValue |= visitReplaceVarRef(rightHandSide, constants);
        } else {
            for(var child : rightHandSide.getDescendants(Kind.VAR_REF_EXPR)) {
                returnValue |= visitReplaceVarRef(child, constants);
            }
            if(getKind(variable).equals(Kind.VAR_REF_EXPR)) {
                constants.remove(variableName);
            }
        }

        return returnValue;
    }

    private Boolean visitArrayAcces(JmmNode arrayAccess, Map<String, String> constants) {
        Boolean returnValue = Boolean.FALSE;
        JmmNode arrayAccessVar = arrayAccess.getChild(1);
        if(Kind.fromString(arrayAccessVar.getKind()).equals(Kind.VAR_REF_EXPR)) {
            return visitReplaceVarRef(arrayAccessVar, constants);
        } else {
            for(var child : arrayAccessVar.getDescendants(Kind.VAR_REF_EXPR)) {
                returnValue |= visitReplaceVarRef(child, constants);
            }
        }
        return returnValue;
    }

    private Boolean visitVarMethod(JmmNode varMethod, Map<String, String> constants) {
        Boolean returnValue = Boolean.FALSE;
        for(var child : varMethod.getDescendants(Kind.VAR_REF_EXPR)) {
            returnValue |= visitReplaceVarRef(child, constants);
        }

        return returnValue;
    }

    private Boolean visitIfElseStmt(JmmNode ifElse, Map<String, String> constants) {
        Boolean returnValue = Boolean.FALSE;
        List<JmmNode> children = ifElse.getChildren();
        JmmNode ifCondition = children.get(0);
        List<JmmNode> ifBody = children.get(1).getChildren();
        List<JmmNode> elseBody = children.get(2).getChildren();

        Kind ifConditionKind = Kind.fromString(ifCondition.getKind());
        if(ifConditionKind.equals(Kind.VAR_REF_EXPR)) {
            returnValue = visitReplaceVarRef(ifCondition, constants);
        } else {
            for(JmmNode varRef : ifCondition.getDescendants(Kind.VAR_REF_EXPR)) {
                returnValue |= visitReplaceVarRef(varRef, constants);
            }
        }

        Map<String, String> ifConstants = new HashMap<>(constants);
        Map<String, String> elseConstants = new HashMap<>(constants);
        returnValue |= visitNodeChildren(ifBody, ifConstants);
        returnValue |= visitNodeChildren(elseBody, elseConstants);
        Set<String> constantKeys = constants.keySet();

        for(String key : constantKeys) {
            if(ifConstants.containsKey(key) && !constants.get(key).equals(ifConstants.get(key))) {
                constants.remove(key); // if has same constant but different value -> cant determine value -> remove it
                continue;
            } else if(!ifConstants.containsKey(key)) {
                constants.remove(key); // if has no constant -> was changed to non-constant -> remove it
                continue;
            }

            if(elseConstants.containsKey(key) && !constants.get(key).equals(elseConstants.get(key))) {
                constants.remove(key); // else has same constant but different value -> cant determine value -> remove it
            } else if(!elseConstants.containsKey(key)) {
                constants.remove(key); // else has no constant -> was changed to non-constant -> remove it
            }
        }

        return returnValue;
    }

    private Boolean visitNodeChildren(List<JmmNode> children, Map<String, String> constants) {
        Boolean returnValue = Boolean.FALSE;
        for(var child : children) {
            String kind = child.getKind();

            switch (Kind.fromString(kind)) {
                case ASSIGN_STMT -> returnValue = visitAssignStmt(child, constants);
                case VAR_METHOD -> returnValue |= visitVarMethod(child, constants);
                case IF_ELSE_STMT -> returnValue |= visitIfElseStmt(child, constants);
                case WHILE_STMT -> returnValue |= visitWhileStmt(child, constants);
                case RETURN_STMT -> returnValue |= visitReturnStmt(child, constants);
            }
        }

        return returnValue;
    }

    private Boolean visitWhileStmt(JmmNode whileStmt, Map<String, String> constants) {
        Boolean returnValue = Boolean.FALSE;
        List<JmmNode> children = whileStmt.getChildren();
        JmmNode whileCondition = children.get(0);
        List<JmmNode> whileBody = children.get(1).getChildren();

        // getting vars assigned in while body and removing from constants
        Set<String> assignedInWhile = getAssignedInNode(children.get(1));
        for(String var : assignedInWhile) {
            constants.remove(var);
        }

        Map<String, String> whileConstants = new HashMap<>(constants);
        Kind whileConditionKind = Kind.fromString(whileCondition.getKind());
        if(whileConditionKind.equals(Kind.VAR_REF_EXPR)) {
            returnValue = visitReplaceVarRef(whileCondition, whileConstants);
        } else {
            for(JmmNode varRef : whileCondition.getDescendants(Kind.VAR_REF_EXPR)) {
                returnValue |= visitReplaceVarRef(varRef, whileConstants);
            }
        }

        returnValue |= visitNodeChildren(whileBody, whileConstants);

        return returnValue;
    }

    private Boolean visitReturnStmt(JmmNode returnStmt, Map<String, String> constants) {
        Boolean returnValue = Boolean.FALSE;

        Kind returnStmtKind = Kind.fromString(returnStmt.getKind());
        if(returnStmtKind.equals(Kind.VAR_REF_EXPR)) {
            returnValue = visitReplaceVarRef(returnStmt, constants);
        } else {
            for(JmmNode varRef : returnStmt.getDescendants(Kind.VAR_REF_EXPR)) {
                returnValue |= visitReplaceVarRef(varRef, constants);
            }
        }

        return returnValue;
    }

    private Boolean visitReplaceVarRef(JmmNode varRef, Map<String, String> constants) {
        if(constants.containsKey(varRef.get("name"))) {
            JmmParserResult result = parser.parse(constants.get(varRef.get("name")),"expr", CompilerConfig.getDefault());
            JmmNode newNode = result.getRootNode();
            varRef.replace(newNode);
            this.changed = true;
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    private Set<String> getAssignedInNode(JmmNode node) {
        Set<String> result = new HashSet<>();
        List<JmmNode> assignStmts = node.getDescendants(Kind.ASSIGN_STMT);

        for(JmmNode assignStmt : assignStmts) {
            JmmNode var = assignStmt.getChild(0);
            if(var.isInstance(Kind.VAR_REF_EXPR))
                result.add(var.get("name"));
        }

        return result;
    }

    private Kind getKind(JmmNode node) {
        return Kind.fromString(node.getKind());
    }

    public boolean hasChanged() {
        return this.changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }
}