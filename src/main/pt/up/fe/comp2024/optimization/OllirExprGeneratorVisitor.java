package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Ollir;
import org.specs.comp.ollir.OperandType;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.analysis.AnalysisUtils;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.sql.SQLOutput;
import java.util.List;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends AJmmVisitor<Void, OllirExprResult> {

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
        addVisit(THIS, this::visitThis);
        addVisit(UNARY, this::visitUnary);
        addVisit(ACCESS_ARRAY, this::visitAccessArray);
        addVisit(NEW_INT, this::visitNewInt);
        addVisit(LENGTH, this::visitLength);
        addVisit(INIT_ARRAY, this::visitInitArray);
        addVisit(UNARY, this::visitUnary);
        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitInitArray(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        String lhsName;
        int size = node.getNumChildren();

        // verifica se é assignment e se for verifica se é um field
        // (fields usam metodos getField e putField, logo nao vao ser tratados como um assign)
        boolean isAssignment = node.getParent().isInstance(ASSIGN_STMT) && !(node.getParent().getChild(0).get("isField").equals("True"));

        if (node.getParent().getChild(0).hasAttribute("name")) {
            lhsName = node.getParent().getChild(0).get("name");
        }
        else {
            lhsName = OptUtils.getTemp();
            code.append(lhsName).append(".array.i32 :=.array.i32 ");
        }

//        if (isAssignment) {
//            lhsName = node.getParent().getChild(0).get("name");
//        }
//        else {
//            lhsName = OptUtils.getTemp();
//            code.append(lhsName).append(".array.i32 :=.array.i32 ");
//        }

        code.append("new(array, ").append(size).append(".i32).array.i32");

        boolean hasParams = false;
        for (int i = 0; i < size; i++) {
            hasParams = true;
            if (i == 0) {
                code.append(";\n");
            }
            OllirExprResult exprVisit = visit(node.getJmmChild(i));
            code.append(exprVisit.getComputation());
            code.append(lhsName).append("[").append(i).append(".i32].i32 :=.i32 ").append(exprVisit.getCode());

            if (i != size-1 || !isAssignment) {
                code.append(";\n");
            }
        }

        if (!isAssignment && !code.toString().endsWith(";") && !hasParams) {
            code.append(";\n");
        }

        return isAssignment ?
                new OllirExprResult(code.toString()) :
                new OllirExprResult(lhsName + ".array.i32", code.toString());
    }

    private OllirExprResult visitLength(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        OllirExprResult exprVisit = visit(node.getJmmChild(0));

        String tempVar = OptUtils.getTemp();

        code.append(exprVisit.getComputation());
        code.append(tempVar);
        code.append(".i32 :=.i32 arraylength(");
        code.append(exprVisit.getCode());
        code.append(").i32;\n");

        return new OllirExprResult(tempVar + ".i32", code.toString());
    }

    private OllirExprResult visitNewInt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        OllirExprResult sizeVisit = visit(node.getJmmChild(0));
        JmmNode parent = node.getParent();

        if (parent.isInstance(ASSIGN_STMT) && parent.getChild(0).get("isField").equals("True")) {
            String temp = OptUtils.getTemp();
            String computation = sizeVisit.getComputation() +
                    temp + ".array.i32 " + ASSIGN + ".array.i32 " + "new(array, " + sizeVisit.getCode() + ").array.i32;\n";

            return new OllirExprResult(temp + ".array.i32", computation);
        }
        else {
            code.append("new(array, ");
            code.append(sizeVisit.getCode());
            code.append(").array.i32");

            return new OllirExprResult(code.toString(), sizeVisit.getComputation());
        }
    }

    private OllirExprResult visitAccessArray(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        String arrayAccessType = OptUtils.toOllirType(TypeUtils.getExprType(node, table));
        JmmNode indexNode = node.getJmmChild(1);
        JmmNode leftNode = node.getJmmChild(0);

        Optional<String> assignLeftOpt = node.getOptional("assignLeft");
        String assignLeft = assignLeftOpt.orElse("False");

        if(assignLeft.equals("True")) {
            String tmpVar = OptUtils.getTemp();

            StringBuilder indexValue = new StringBuilder();

            if(!indexNode.getKind().equals("IntegerLiteral")) {
                // get code and computation of method

                OllirExprResult result = visit(indexNode);
                computation.append(result.getComputation());
                indexValue.append(result.getCode());
            } else {
                indexValue.append(indexNode.get("value")).append(arrayAccessType);
            }

            code.append(leftNode.get("name")).append("[").append(indexValue).append("]").append(arrayAccessType);
        } else {
            String tmpVar = OptUtils.getTemp();

            StringBuilder indexValue = new StringBuilder();

            if(!indexNode.getKind().equals("IntegerLiteral")) {
                // get code and computation of method
                OllirExprResult result = visit(indexNode);
                computation.append(result.getComputation());
                indexValue.append(result.getCode());
            } else {
                indexValue.append(indexNode.get("value")).append(arrayAccessType);
            }

            OllirExprResult leftNodeOllir = visit(leftNode);
            computation.append(leftNodeOllir.getComputation());

            computation.append(tmpVar).append(arrayAccessType).append(" :=").append(arrayAccessType).append(SPACE);
            computation.append(leftNodeOllir.getCode());
            computation.append("[").append(indexValue).append("]").append(arrayAccessType).append(";\n");
            code.append(tmpVar).append(arrayAccessType);
        }

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitUnary(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        var ret = visit(node.getJmmChild(0));
        code.append(ret.getComputation());

        String tempVar = OptUtils.getTemp();
        code.append(tempVar).append(".bool :=.bool !.bool ").append(ret.getCode()).append(";").append("\n");

        return new OllirExprResult(tempVar + ".bool", code);
    }

    private OllirExprResult visitThis(JmmNode node, Void unused) {
        return new OllirExprResult("this." + table.getClassName());
    }

    private OllirExprResult visitParenthesis(JmmNode node, Void unused) {
        return visit(node.getJmmChild(0));
    }

    private OllirExprResult visitVarMethod(JmmNode node, Void unused) {
        String methodName = node.get("name");
        JmmNode callerNode;

        if (node.getJmmChild(0).isInstance(PARENTHESIS))
            callerNode = node.getJmmChild(0).getJmmChild(0);
        else
            callerNode = node.getJmmChild(0);

        List<String> imports = table.getImports();
        StringBuilder computation = new StringBuilder();
        String callerType = OptUtils.toOllirType(TypeUtils.getExprType(callerNode, table));
        String callerName, methodReturnType;

        // variaveis para caso seja um assignment;
        JmmNode parent = node.getParent();
        String invokeType;
        String tempVar = "";
        System.out.println("node:" + node.toString() + "\ncaller:\t" + callerNode.toString());
        // this.foo() ou className classname; classname.foo()
        if (callerNode.isInstance(THIS) ||
                (callerNode.isInstance(VAR_REF_EXPR) && callerType.equals("." + table.getClassName())) ||
                (callerNode.isInstance(VAR_METHOD) && table.getMethods().contains(callerNode.get("name")))) {
            System.out.println("ENTRA\nnode:" + node.toString() + "\ncaller:\t" + callerNode.toString());
            System.out.println("node:" + node.toString() + "\ntype:\t" + callerType);
            System.out.println("node:" + node.toString() + "\nret type:\t"+table.getReturnType(methodName));
            invokeType = "invokevirtual";
            callerName = callerNode.isInstance(THIS) ? "this" : (callerNode.get("name") + callerType);
            methodReturnType = OptUtils.toOllirType(table.getReturnType(methodName));
            System.out.println("node:" + node.toString() + "\n" +methodReturnType);
        }
        else if (callerNode.isInstance(NEW_CLASS)) {
            // constroi NEW em OLLIR
            var ret = visitClassInstantiation(callerNode, null);
            computation.append(ret.getComputation());

            // cria var temporaria
            invokeType = "invokevirtual";
            callerName = ret.getCode();

            // se new X criar um elemento da classe atual, obtem return type
            if (callerNode.get("name").equals(table.getClassName())) {
                methodReturnType = OptUtils.toOllirType(table.getReturnType(node.get("name")));
            }
            else {  // new X de um import.
                methodReturnType = "." + callerNode.get("name");
            }

        }
        else if(imports.contains(callerNode.get("name"))) { // A.foo()
            invokeType = "invokestatic";
            callerName = callerNode.get("name");
            methodReturnType = ".V";
        }
        else { // A a; a.foo();
            invokeType = "invokevirtual";
            callerName = callerNode.get("name") + callerType;
            methodReturnType = ".V";
        }
        System.out.println("node:" + node.toString() + "\nmid:\t" + methodReturnType);
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

                Optional<String> name = lhs.getOptional("name");
                if(name.isEmpty()) name = lhs.getOptional("var");

                Optional<Symbol> leftType = AnalysisUtils.validateSymbolFromSymbolTable(table, name.get());
                if (leftType.isPresent()) {
                    methodReturnType = OptUtils.toOllirType(TypeUtils.getExprType(lhs, table));
                }
            }
            else if (parent.isInstance(BINARY_EXPR)) {
                methodReturnType = OptUtils.toOllirType(TypeUtils.getOperatorOperandsType(parent));
            }
            else if (parent.isInstance(RETURN_STMT)) {
                var currentMethodNodeOpt = node.getAncestor(METHOD_DECL);
                var currentMethodName = currentMethodNodeOpt.orElse(null);

                if (currentMethodName != null) {
                    var returnNode = currentMethodName.getJmmChild(0);
                    String finalType = OptUtils.toOllirType(returnNode);
                    methodReturnType = returnNode.isInstance(ARRAY_TYPE) ? (".array" + finalType) : finalType;
                }
            }
            else if (parent.isInstance(ACCESS_ARRAY)) {
                // retorna array

                if (parent.getChild(0).isInstance(VAR_METHOD)) {
                    methodReturnType = ".array.i32";
                }
                else {
                    methodReturnType = ".i32";
                }
            }
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

        System.out.println("node:" + node.toString() + "\nFINAL:\t" + computation.toString());

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

    private OllirExprResult visitAndShortCircuit(JmmNode node, Void unused) {
        JmmNode firstCondition = node.getJmmChild(0);
        JmmNode secondCondition = node.getJmmChild(1);

        OllirExprResult firstConditionResult = this.visit(firstCondition);
        OllirExprResult secondConditionResult = this.visit(secondCondition);

        String tmpVar = OptUtils.getTemp();

        int labelNumber = Integer.valueOf(node.get("ifLabel"));

        StringBuilder computation = new StringBuilder();
        computation.append(firstConditionResult.getComputation());
        computation.append("if(").append(firstConditionResult.getCode()).append(") goto if_label_").append(labelNumber).append(";").append("\n");
        computation.append(String.format("%s.bool :=.bool 0.bool;", tmpVar)).append("\n");
        computation.append("goto endif_").append(labelNumber).append(";\n");

        computation.append("if_label_").append(labelNumber).append(":").append("\n");
        computation.append(secondConditionResult.getComputation());

        computation.append(tmpVar).append(".bool :=.bool ").append(secondConditionResult.getCode()).append(";\n");
        computation.append("endif_").append(labelNumber).append(":").append("\n");

        return new OllirExprResult(tmpVar + ".bool", computation.toString());
    }

    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {
        if(node.get("op").equals("&&")) {
            return this.visitAndShortCircuit(node, unused);
        }

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
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        if(node.get("isField").equals("True")) {
            if(node.get("assignLeft").equals("False")) {
                var tmp = OptUtils.getTemp();
                computation.append(tmp).append(ollirType).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE);
                computation.append("getfield(this, ").append(id).append(ollirType).append(")").append(ollirType).append(END_STMT);
                code.append(tmp).append(ollirType);
            }
        } else {
            code.append(id).append(ollirType);
        }

        return new OllirExprResult(code.toString(), computation.toString());
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
