package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    int currentMethodVirtualReg = 1;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Field.class, this::generateField);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(CallInstruction.class, this::generateCall);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
    }

    private String generateField(Field field) {
        StringBuilder code = new StringBuilder();

        code.append(".field");
        code.append(" ");
        code.append("private");
        code.append(" ");
        code.append(field.getFieldName());
        code.append(" ");
        code.append(JasminMethodUtils.getTypeInJasminFormat(field.getFieldType()));
        code.append(NL);

        return code.toString();
    }


    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class public ").append(className).append(NL).append(NL);

        String superClass = "java/lang/Object";

        if (classUnit.getSuperClass() != null) {
            for (String imp : classUnit.getImports()) {
                String[] impParsed = imp.split("\\.");

                if (impParsed[impParsed.length - 1].equals(classUnit.getSuperClass())) {
                    superClass = imp.replace(".", "/");
                }
            }
        }

        code.append(".super ").append(superClass).append(NL);

        for(var field : classUnit.getFields()) {
            code.append(generators.apply(field));
        }

        String superConstructorInvokerString = "invokespecial " + superClass + "/<init>()V";

        // generate a single constructor method
        var defaultConstructor = String.format("""
                ;default constructor
                .method public <init>()V
                    aload_0
                    %s
                    return
                .end method
                """, superConstructorInvokerString);
        code.append(defaultConstructor);

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }

            code.append(generators.apply(method));
        }

        return code.toString();
    }

    private String generateMethod(Method method) {
        // set method
        currentMethod = method;
        this.currentMethodVirtualReg = 1;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var methodName = method.getMethodName();

        if(methodName.equals("main")) {
            modifier = modifier + "static ";
        }

        // TODO: Hardcoded param types and return type, needs to be expanded

        // String with the types of the parameters
        StringBuilder paramString = new StringBuilder();
        paramString.append("(");

        ArrayList<Element> params = method.getParams();
        for(int i = 0; i < params.size(); i++) {
            Element param = params.get(i);
            paramString.append(JasminMethodUtils.getTypeInJasminFormat(param.getType()));
        }
        paramString.append(")");

        code.append("\n.method ").append(modifier).append(methodName).append(paramString.toString() + JasminMethodUtils.getTypeInJasminFormat(method.getReturnType())).append(NL);

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        code.append(
            switch (operand.getType().toString()) {
                case "INT32", "INT", "BOOLEAN" -> "istore";
                default -> "astore";
            }
        ).append(reg < 4 ? "_" : " ").append(reg).append(NL);

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        return switch (operand.getType().toString()) {
            case "INT32", "INT", "BOOLEAN" -> "iload";
            default -> "aload";
        } + (reg < 4 ? "_" : " ") + reg + NL;
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case MUL -> "imul";
            case SUB -> "isub";
            case DIV -> "idiv";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        boolean voidReturningMethod = false;
        if(returnInst.getOperand() == null) {
            voidReturningMethod = true;
        }

        if(!voidReturningMethod) {
            code.append(generators.apply(returnInst.getOperand()));
            code.append("ireturn").append(NL);
        } else if(voidReturningMethod) {
            code.append("return");
        } else {
            code.append("areturn");
        }

        return code.toString();
    }

    private String generateCall(CallInstruction callInst) {
        StringBuilder code = new StringBuilder();

        CallType invocationType = callInst.getInvocationType();

        switch (invocationType) {
            case invokestatic -> generateInvokeStatic(callInst, code);
            case invokevirtual -> generateInvokeVirtual(callInst, code);
            case NEW -> generateNew(callInst, code);
            case invokespecial -> generateSpecial(callInst, code);
            default -> throw new NotImplementedException("Invocation type " + invocationType.name() + " not implemented in CallInstruction");
        }

        return code.toString();
    }

    private void generateInvokeStatic(CallInstruction callInst, StringBuilder code) {
        String argumentsType = pushArgumentsIntoStackAndGetType(callInst, code);

        code.append(callInst.getInvocationType().toString());
        code.append(" ");
        code.append(callInst.getOperands().get(0).toString().split(": ")[1].split("\\.")[0]);
        code.append("/");

        Pattern pattern = Pattern.compile("\"(.*)\"");
        Matcher matcher = pattern.matcher(callInst.getMethodName().toString());

        if (matcher.find())
            code.append(matcher.group(1));

        code.append("(");
        code.append(argumentsType);
        code.append(")");

        code.append(JasminMethodUtils.getTypeInJasminFormat(callInst.getReturnType()));
        code.append(NL);
    }

    private void generateInvokeVirtual(CallInstruction callInst, StringBuilder code) {

        //push caller na stack
        code.append(generators.apply(callInst.getOperands().get(0)));

        //push arguments na stack
        String argumentTypes = pushArgumentsIntoStackAndGetType(callInst, code);

        code.append(callInst.getInvocationType().toString());
        code.append(" ");
        code.append(JasminMethodUtils.getTypeInJasminFormat(callInst.getCaller().getType()));
        code.append("/");

        Pattern pattern = Pattern.compile("\"(.*)\"");
        Matcher matcher = pattern.matcher(callInst.getMethodName().toString());

        if (matcher.find())
            code.append(matcher.group(1));

        code.append("(");
        code.append(argumentTypes);
        code.append(")");

        code.append(JasminMethodUtils.getTypeInJasminFormat(callInst.getReturnType()));
        code.append(NL);
    }

    private void generateNew(CallInstruction callInst, StringBuilder code) {
        code.append("new ");
        code.append(callInst.getOperands().get(0).toString().split(": ")[1].split("\\.")[0]);
        code.append(NL);
        code.append("dup");
        code.append(NL);
    }

    private void generateSpecial(CallInstruction callInst, StringBuilder code) {
        code.append(generators.apply(callInst.getOperands().get(0)));
        code.append("invokespecial ");
        code.append(JasminMethodUtils.getTypeInJasminFormat(callInst.getOperands().get(0).getType()));
        code.append("/");
        code.append("<init>()V");
        code.append(NL);
        code.append("pop");
        code.append(NL);
    }

    // da push dos arguments na stack e retorna uma string com os tipos dos argumentos
    private String pushArgumentsIntoStackAndGetType(CallInstruction inst, StringBuilder code) {
        StringBuilder argumentsType = new StringBuilder();

        if (inst.getArguments().size() > 0) {
            inst.getArguments().forEach(argument -> {
                code.append(generators.apply(argument));
                argumentsType.append(JasminMethodUtils.getTypeInJasminFormat(argument.getType()));
            });
        }

        return argumentsType.toString();
    }


    private String generatePutField(PutFieldInstruction putFieldInstruction) {
        StringBuilder code = new StringBuilder();

        code.append("aload_0").append(NL);
        code.append(generators.apply(putFieldInstruction.getOperands().get(2)));

        code.append("putfield");
        code.append(" ");
        code.append(ollirResult.getOllirClass().getClassName());
        code.append("/");
        code.append(putFieldInstruction.getField().getName());
        code.append(" ");
        code.append(JasminMethodUtils.getTypeInJasminFormat(putFieldInstruction.getField().getType()));
        code.append(NL);

        return code.toString();
    }

    private String generateGetField(GetFieldInstruction getFieldInstruction) {
        StringBuilder code = new StringBuilder();

        code.append("aload_0").append(NL);

        code.append("getfield");
        code.append(" ");
        code.append(ollirResult.getOllirClass().getClassName());
        code.append("/");
        code.append(getFieldInstruction.getField().getName());
        code.append(" ");
        code.append(JasminMethodUtils.getTypeInJasminFormat(getFieldInstruction.getField().getType()));
        code.append(NL);

        return code.toString();
    }
}
