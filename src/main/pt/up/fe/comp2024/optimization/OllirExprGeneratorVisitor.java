package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.analysis.AnalysisUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(BOOL, this::visitBoolLiteral);
        addVisit(NEW_CLASS, this::visitClassInstantiation);
        addVisit(VAR_METHOD, this::visitVarMethod);
        addVisit(PARENTHESIS, this::visitParenthesis);


        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitParenthesis(JmmNode node, Void unused) {
        return visit(node.getJmmChild(0));
    }

    private OllirExprResult visitVarMethod(JmmNode node, Void unused) {
        String methodName = node.get("name");
        JmmNode callerNode = node.getJmmChild(0);
        List<String> imports = table.getImports();
        StringBuilder computation = new StringBuilder();
        String callerType = OptUtils.toOllirType(TypeUtils.getExprType(callerNode, table));
        String callerName, methodReturnType;

        // variaveis para caso seja um assignment;
        JmmNode parent = node.getParent();
        String invokeType;
        String tempVar = "";

        // this.foo()
        if (callerNode.isInstance(THIS)) {
            invokeType =  "invokevirtual";
            callerName = "this" + callerType;
            methodReturnType = OptUtils.toOllirType(table.getReturnType(methodName));
        }
        else if(imports.contains(callerNode.get("name"))) { // A.foo()
            invokeType = "invokestatic";
            callerName = callerNode.get("name");
            methodReturnType = ".V";
        }
        else { // A a; a.foo();
            invokeType = "invokevirtual";
            callerName = callerNode.get("name") + callerType;
            methodReturnType = callerType;
        }

        StringBuilder params = new StringBuilder();
        // tem parametros
        if (node.getNumChildren() > 1) {
            for (int i = 1; i < node.getNumChildren(); i++) {
                JmmNode child = node.getJmmChild(i);
                params.append(", ");
                OllirExprResult result = visit(child);

                // adiciona código extra caso param seja uma BINARY_EXPR ou VAR_METHOD
                computation.append(result.getComputation());

                params.append(result.getCode());
            }
        }

        // é um assign / operacao
        if (!parent.isInstance(EXPR_STMT)) {
            tempVar = OptUtils.getTemp();

            if (parent.isInstance(ASSIGN_STMT)) {
                JmmNode lhs = parent.getChild(0);
                Optional<Symbol> leftType = AnalysisUtils.validateSymbolFromSymbolTable(table, lhs.get("name"));
                if (leftType.isPresent()) {
                    methodReturnType = OptUtils.toOllirType(TypeUtils.getExprType(lhs, table));
                }
            }
//            else {
//                methodReturnType = OptUtils.toOllirType(TypeUtils.getExprType(parent, table));
//            }

            computation.append(String.format("%s%s :=%s ", tempVar, methodReturnType, methodReturnType));
        }

        computation.append(invokeType);
        computation.append("(");
        computation.append(callerName);
        computation.append(", \"");
        computation.append(methodName);
        computation.append("\"");

        if (params.length() > 0) {
            computation.append(params);
        }

        computation.append(")");
        computation.append(methodReturnType);
        computation.append(";");
        computation.append("\n");

        if (!parent.isInstance(EXPR_STMT)) {
            return new OllirExprResult(String.format("%s%s", tempVar, methodReturnType), computation.toString());
        }

        return new OllirExprResult(computation.toString());
    }

    private OllirExprResult visitClassInstantiation(JmmNode node, Void unused) {

        StringBuilder computation = new StringBuilder();
        String nodeType = OptUtils.toOllirType(node);
        String tempVar = OptUtils.getTemp();

        computation.append(String.format("%s%s :=%s new(%s)%s;\n", tempVar, nodeType, nodeType, node.get("name"), nodeType));
        computation.append(String.format("invokespecial(%s%s, \"<init>\").V;\n", tempVar, nodeType));

        String code = String.format("%s%s", tempVar, nodeType);

        return new OllirExprResult(code, computation.toString());
    }

    private OllirExprResult visitBoolLiteral(JmmNode node, Void unused) {
        String boolLiteralValue = node.get("value");
        String[] boolValues = {"false", "true"};

        StringBuilder code = new StringBuilder();

        for(int i = 0; i < boolValues.length; i++) {
            if(boolValues[i].equals(boolLiteralValue)) {
                code.append(String.format("%d", i));
                break;
            }
        }

        code.append(".bool");
        return new OllirExprResult((code.toString()));
    }

    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table);
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        var id = node.get("name");
        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        String code = id + ollirType;

        return new OllirExprResult(code);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
