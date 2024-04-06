package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
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
        addVisit(VAR_METHOD, this::visitVarMethod);
        setDefaultVisit(this::defaultVisit);
    }

    private String visitImport(JmmNode node, Void unused) {
        String imports = node.get("names").replaceAll("[\\[\\]\\s]", "").replaceAll(",",".");
        System.out.println(imports);
        return "import " + String.join(".", imports) + END_STMT;
    }

    private String visitVarMethod(JmmNode node, Void unused) {
        String methodName = node.get("name");
        JmmNode callerNode = node.getJmmChild(0);
        List<String> imports = table.getImports();
        StringBuilder code = new StringBuilder();

        String callerType = OptUtils.toOllirType(TypeUtils.getExprType(callerNode, table));
        String callerName, methodReturnType;

        // this.foo()
        if (callerNode.isInstance(THIS)) {
            code.append("invokevirtual");
            callerName = "this" + callerType;
            methodReturnType = OptUtils.toOllirType(table.getReturnType(methodName));
        }
        else if(imports.contains(callerNode.get("name"))) { // A.foo()
            code.append("invokestatic");
            callerName = callerNode.get("name");
            methodReturnType = "." + callerNode.get("name");
        }
        else { // A a; a.foo();
            code.append("invokevirtual");
            callerName = callerNode.get("name") + callerType;
            methodReturnType = callerType;
        }

        code.append("(");
        code.append(callerName);
        code.append(", \"");
        code.append(methodName);
        code.append("\")");
        code.append(methodReturnType);
        code.append(";");
        code.append(NL);

        return code.toString();
    }

    private String visitInitArray(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        return "h";
    }

    private String visitAssignStmt(JmmNode node, Void unused) {

        var lhs = exprVisitor.visit(node.getJmmChild(0));
        var rhs = exprVisitor.visit(node.getJmmChild(1));

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(lhs.getComputation());
        code.append(rhs.getComputation());

        // code to compute self
        // statement has type of lhs
        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
        String typeString = OptUtils.toOllirType(thisType);

        code.append(lhs.getCode());
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

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
        code.append(L_BRACKET);

        code.append(NL);
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
