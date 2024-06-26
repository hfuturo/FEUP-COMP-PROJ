package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisUtils;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";
    private int lBranchCounter = 0;
    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }

    @Override
    protected void buildVisitor() {
        addVisit(PROGRAM, this::visitProgram);
        addVisit(IMPORT, this::visitImport);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(INIT_ARRAY, this::visitInitArray);
        addVisit(EXPR_STMT, this::visitExprStmt);
        addVisit(SCOPE_STMT, this::visitScopeStmt);
        addVisit(IF_ELSE_STMT, this::visitIfElseStmt);
        addVisit(WHILE_STMT, this::visitWhileStmt);
        addVisit(INNER_MAIN_METHOD, this::visitMainMethod);
        setDefaultVisit(this::defaultVisit);
    }

    private String visitWhileStmt(JmmNode jmmNode, Void unused) {
        StringBuilder code = new StringBuilder();
        List<JmmNode> children = jmmNode.getChildren();
        String whileLabel = jmmNode.get("whileLabel");

        JmmNode expressionToEvaluate = children.get(0);
        JmmNode whileBody = children.get(1);

        code.append(String.format("whileCond%s:", whileLabel)).append(NL);

        OllirExprResult expressionToEvaluateOllir = this.exprVisitor.visit(expressionToEvaluate);

        code.append(expressionToEvaluateOllir.getComputation());
        code.append(String.format("if(%s) goto whileLoop%s;", expressionToEvaluateOllir.getCode(), whileLabel)).append(NL);
        code.append(String.format("goto whileEnd%s;", whileLabel)).append(NL);

        code.append(String.format("whileLoop%s:", whileLabel)).append(NL);

        code.append(visit(whileBody));

        code.append(String.format("goto whileCond%s;", whileLabel)).append(NL);
        code.append(String.format("whileEnd%s:", whileLabel)).append(NL);

        return code.toString();
    }

    private String visitScopeStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        List<JmmNode> children = node.getChildren();

        for(JmmNode child: children) {
            code.append(visit(child));
        }

        return code.toString();
    }

    private String visitIfElseStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        List<JmmNode> children = node.getChildren();

        JmmNode expressionToEvaluate = children.get(0);
        JmmNode statementToExecuteIfIfCondIsTrue = children.get(1);
        JmmNode statementToExecuteIfIfCondIsFalse = children.get(2);

        OllirExprResult expressionToEvaluateOllir = this.exprVisitor.visit(expressionToEvaluate);
        code.append(expressionToEvaluateOllir.getComputation());
        code.append(this.buildIfCode(node, expressionToEvaluateOllir.getCode()));

        code.append(visit(statementToExecuteIfIfCondIsFalse));
        code.append(String.format("goto if_end_%s;", node.get("ifLabel"))).append(NL);

        code.append(String.format("if_then_%s:", node.get("ifLabel"))).append(NL);
        code.append(visit(statementToExecuteIfIfCondIsTrue));

        code.append(String.format("if_end_%s:", node.get("ifLabel"))).append(NL);
        return code.toString();
    }

    private String buildIfCode(JmmNode node, String code) {
        StringBuilder result = new StringBuilder();

        result.append(String.format("if(%s) goto if_then_%s;", code, node.get("ifLabel"))).append(NL);
        lBranchCounter += 1;

        return result.toString();
    }

    private String visitExprStmt(JmmNode node, Void unused) {
        return exprVisitor.visit(node.getJmmChild(0)).getCode();
    }

    private String visitMainMethod(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder(".method ");
        code.append("public static main(args.array.String).V {\n");

        var params = node.getChildren(PARAM);

        // rest of its children stmts
        var afterParam = params.size();
        for (int i = afterParam; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            if(Kind.fromString(child.getKind()).equals(PARAM)) {
                continue;
            }
            var childCode = visit(child);
            code.append(childCode);
        }

        code.append("ret.V;\n");
        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }

    private String visitImport(JmmNode node, Void unused) {
        String imports = node.get("names").replaceAll("[\\[\\]\\s]", "").replaceAll(",",".");
        return "import " + imports + END_STMT;
    }

    private String visitInitArray(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        return "h";
    }

    private String visitAssignStmt(JmmNode node, Void unused) {
        JmmNode left = node.getJmmChild(0);
        JmmNode right = node.getJmmChild(1);

        var lhs = exprVisitor.visit(left);
        var rhs = exprVisitor.visit(right);

        String computation = rhs.getComputation();
        String[] computationLines = computation.split("\n");
        boolean iincAssign = (computationLines.length == 1 && !computationLines[0].isEmpty());

        StringBuilder code = new StringBuilder();
        boolean rightIsBinaryExpr = right.getKind().equals("BinaryExpr");

        // code to compute the children
        if(!iincAssign) {
            code.append(lhs.getComputation());
            code.append(rhs.getComputation());
        }

        // code to compute self
        // statement has type of lhs
        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
        String typeString = OptUtils.toOllirType(thisType);

        if(node.getJmmChild(0).isInstance(ACCESS_ARRAY) && node.getJmmChild(0).getJmmChild(0).get("isField").equals("True")) {
            var accessArray = node.getJmmChild(0);
            var varRef = accessArray.getJmmChild(0);
            var varRefVisit = exprVisitor.visit(varRef);
            var access = lhs.getCode();

            code.append(varRefVisit.getCode());
            code.append(SPACE);
            code.append(ASSIGN).append(typeString);
            code.append(SPACE);
            code.append("getfield(this, ").append(varRef.get("name")).append(".array").append(typeString).append(")").append(".array").append(typeString).append(END_STMT);

            code.append(varRefVisit.getCode(), 0, varRefVisit.getCode().indexOf('.'));
            code.append('[').append(access, access.indexOf('[') + 1, access.indexOf(']')).append(']').append(typeString);
            code.append(SPACE).append(ASSIGN).append(typeString).append(SPACE).append(rhs.getCode()).append(END_STMT);

        } else if(node.getJmmChild(0).get("isField").equals("True")) {
            var jmmNode = node.getJmmChild(0);
            var leftChild = node.getJmmChild(0);
            code.append("putfield(this, ").append(leftChild.get("name")).append(typeString).append(", ").append(rhs.getCode()).append(").V;\n");
        } else {
            code.append(lhs.getCode());
            code.append(SPACE);

            code.append(ASSIGN);
            code.append(typeString);
            code.append(SPACE);

            if(iincAssign) {
                String[] computationComponents = rhs.getComputation().split(String.format(":=%s", typeString));
                code.append(computationComponents[1]);
            } else {
                code.append(rhs.getCode());
                code.append(END_STMT);
            }

        }

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {
        Optional<String> methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name"));
        if(methodName.isEmpty()) {
            methodName = Optional.of("main");
        }

        Type retType = table.getReturnType(methodName.get());

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));
        }

        code.append(expr.getComputation());
        code.append("ret");
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");

        String code = id + typeCode;

        return code;
    }


    private String visitMethodDecl(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");

        if (isPublic) {
            code.append("public ");
        }

        if (node.hasAttribute("hasVarargs") && node.get("hasVarargs").equals("True")) {
            code.append("varargs ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        // param
        List<JmmNode> params = node.getChildren(PARAM);
        code.append("(");
        for(int i = 0; i < params.size(); i++) {
            JmmNode param = params.get(i);
            code.append(visit(param));

            if(i < (params.size() - 1)) code.append(", ");
        }
        code.append(")");

        // type
        JmmNode returnTypeNode = node.getJmmChild(0);
        var retType = OptUtils.toOllirType(returnTypeNode);
        code.append(retType);
        code.append(L_BRACKET);


        // rest of its children stmts
        var afterParam = params.size();
        for (int i = afterParam; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            if(Kind.fromString(child.getKind()).equals(PARAM)) {
                continue;
            }
            var childCode = visit(child);
            code.append(childCode);
        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }


    private String visitClass(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());

        if(!table.getSuper().isEmpty()) {
            code.append(SPACE).append("extends").append(SPACE).append(table.getSuper()).append(SPACE);
        }
        code.append(L_BRACKET);
        code.append(NL);

        for(JmmNode varDecl : node.getChildren(VAR_DECL)) {
            String name = varDecl.get("name");
            String type = OptUtils.toOllirType(varDecl.getChild(0));

            code.append(".field").append(SPACE).append("public").append(SPACE).append(name).append(type).append(";").append(NL);
        }

        var needNl = true;

        for (var child : node.getChildren()) {
            var result = visit(child);

            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {

        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }


    private String visitProgram(JmmNode node, Void unused) {
        this.lBranchCounter = 0;

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        for (var child : node.getChildren()) {
            code.append(visit(child));
        }

        return code.toString();
    }
}
