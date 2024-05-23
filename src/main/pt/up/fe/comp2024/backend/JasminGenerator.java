package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;
    private int maxConstantPoolValue = 5;
    private int minConstantPoolValue = 0;
    private int stack = 1;
    private int currentLthLabel = 0;
    private int currentStackValue = 0;
    List<String> classUnitImports;
    List<Report> reports;

    String code;

    Method currentMethod;

    int currentMethodVirtualReg = 1;

    private final FunctionClassMap<TreeNode, String> generators;
    private boolean currentCallInstructionIsOnAssign;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.classUnitImports = new ArrayList<>();

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Field.class, this::generateField);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(SingleOpCondInstruction.class, this::generateSingleOpCond);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(CallInstruction.class, this::generateCall);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(GotoInstruction.class, this::generateGotoInst);
        generators.put(UnaryOpInstruction.class, this::generateUnaryOpInst);
        generators.put(OpCondInstruction.class, this::generateOpCondInst);

        this.currentCallInstructionIsOnAssign = false;
    }

    private String generateOpCondInst(OpCondInstruction opCondInstruction) {
        StringBuilder code = new StringBuilder();

        code.append(this.generators.apply(opCondInstruction.getCondition()));

        code.append("ifne ").append(opCondInstruction.getLabel()).append(NL);

        return code.toString();
    }

    private String generateUnaryOpInst(UnaryOpInstruction unaryOpInstruction) {
        StringBuilder code = new StringBuilder();
        code.append(generators.apply(unaryOpInstruction.getOperand()));
        code.append("ifeq ").append(this.getCmpTrueLabel(this.currentLthLabel)).append(NL);
        code.append("iconst_0").append(NL);
        code.append("goto ").append(this.getCmpEndLabel(this.currentLthLabel)).append(NL);

        code.append(this.getCmpTrueLabel(this.currentLthLabel)).append(":").append(NL);
        code.append("iconst_1").append(NL);

        // Store 1 into the condition
        code.append(this.getCmpEndLabel(this.currentLthLabel)).append(":").append(NL);

        this.increaseLimitStack();
        this.currentLthLabel++;

        return code.toString();
    }

    private String generateGotoInst(GotoInstruction gotoInst) {
        StringBuilder code = new StringBuilder();

        code.append("goto ").append(gotoInst.getLabel()).append(NL).toString();

        return code.toString();
    }

    private String generateSingleOpCond(SingleOpCondInstruction instruction) {
        StringBuilder code = new StringBuilder();

        Element operand = instruction.getOperands().get(0);
        if(operand instanceof LiteralElement) {
            code.append(generators.apply(operand));
        } else if(operand instanceof Operand) {
            code.append(this.generateLoadIndexInstruction(this.intOrReferenceLoad((Operand) operand), (Operand) operand)).append(NL);
        }

        // 2. Verificar se está a 1 (ifne) e se estiver, avançar para o statement dentro do if
        code.append("ifne ").append(instruction.getLabel()).append(NL);

        return code.toString();
    }

    private String intOrReferenceLoad(Operand operand) {
        Type type = operand.getType();

        if (type.toString().equals("BOOLEAN") || type.toString().equals("INT32")) {
            return "iload";
        }

        return "aload";
    }

    private String generateLoadIndexInstruction(String instructionStart, Operand operand) {
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        return instructionStart + (reg < 4 ? "_" : " ") + reg + NL;
    }

    private String generateField(Field field) {
        StringBuilder code = new StringBuilder();

        code.append(".field");
        code.append(" ");
        code.append("public");
        code.append(" ");
        code.append(field.getFieldName());
        code.append(" ");
        code.append(JasminMethodUtils.getTypeInJasminFormat(field.getFieldType(), this.classUnitImports));
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

        this.classUnitImports = classUnit.getImports();
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
        this.currentMethod = method;
        this.currentMethodVirtualReg = 1;
        this.stack = 1;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var methodName = method.getMethodName();

        if(methodName.equals("main")) {
            modifier = modifier + "static ";
        }

        // String with the types of the parameters
        StringBuilder paramString = new StringBuilder();
        paramString.append("(");

        ArrayList<Element> params = method.getParams();
        for(int i = 0; i < params.size(); i++) {
            Element param = params.get(i);
            paramString.append(JasminMethodUtils.getTypeInJasminFormat(param.getType(), this.classUnitImports));
        }
        paramString.append(")");

        code.append("\n.method ").append(modifier).append(methodName).
                append(paramString.toString() +
                        JasminMethodUtils.getTypeInJasminFormat(method.getReturnType(), this.classUnitImports)).
                append(NL);

        StringBuilder instructions = new StringBuilder();
        for (var inst : method.getInstructions()) {
            for (Map.Entry<String, Instruction> entry : method.getLabels().entrySet()) {
                Instruction currentInstruction = entry.getValue();
                if(currentInstruction.equals(inst)) {
                    instructions.append(entry.getKey()).append(":").append(NL);
                }
            }

            for(String instruction: StringLines.getLines(generators.apply(inst))) {
                if(!instruction.matches("cmp_\\d+_((true)|(end))_label:")) {
                    instructions.append(TAB);
                }

                instructions.append(instruction).append(NL);
            }

            //instructions.append(instCode);
        }

        int locals = this.getLimitLocals(method);

        code.append(TAB).append(".limit stack " + this.stack).append(NL);
        code.append(TAB).append(".limit locals " + locals).append(NL);

        code.append(instructions);

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private int getLimitLocals(Method method) {
        int reservedRegistersForThis = method.isStaticMethod() ? 0 : 1;
        Set<Integer> repeatedValues = new HashSet<>();
        for(Map.Entry<String, Descriptor> entry: method.getVarTable().entrySet()) {
            if(entry.getKey().equals("this")) continue;

            repeatedValues.add(entry.getValue().getVirtualReg());
        }

        return reservedRegistersForThis + repeatedValues.size();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        if(assign.getRhs().getInstType().name().equals(InstructionType.CALL.name())) {
            this.currentCallInstructionIsOnAssign = true;
        }

        // 1. Se o rhs tiver apenas uma referência ao meu registo e o resto for tudo operandos literais, colocar o iinc
        if(assign.getRhs().getInstType().name().equals(InstructionType.BINARYOPER.name()) && this.canUseIinc(assign)) {
            this.generateIincAssign(assign, code);
            return code.toString();
        }

        // store value in the stack in destination
        var lhs = assign.getDest();
        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        if (lhs instanceof ArrayOperand) {
            this.generateArrayAssign(assign, code);
        }
        else {
            this.generateNormalAssign(assign, code);
        }

        this.decreaseLimitStack();

        return code.toString();
    }

    private boolean canUseIinc(AssignInstruction assign) {
        if(assign.getRhs().getChildren().size() != 2) return false;

        OperationType opType  = ((BinaryOpInstruction) assign.getRhs()).getOperation().getOpType();
        if(!opType.name().equals("SUB") && !opType.name().equals("ADD")) return false;

        Instruction rightHandSide = assign.getRhs();

        Operand dest = (Operand) assign.getDest();
        TreeNode leftOperand = rightHandSide.getChildren().get(0);
        TreeNode rightOperand = rightHandSide.getChildren().get(1);

        var varTable = this.currentMethod.getVarTable();
        if((leftOperand.getClass() == LiteralElement.class) && (rightOperand.getClass() == Operand.class)) {
            int literal = Integer.parseInt(((LiteralElement) leftOperand).getLiteral());
            boolean negativeMax = (opType.name().equals("SUB") && (literal <= 128));
            boolean positiveMax = (opType.name().equals("ADD") && (literal <= 127));
            return (negativeMax || positiveMax) && this.varHasReg((Operand) rightOperand, varTable, varTable.get(dest.getName()).getVirtualReg());
        }

        if((rightOperand.getClass() == LiteralElement.class) && (leftOperand.getClass() == Operand.class)) {
            int literal = Integer.parseInt(((LiteralElement) rightOperand).getLiteral());
            boolean negativeMax = (opType.name().equals("SUB") && (literal <= 128));
            boolean positiveMax = (opType.name().equals("ADD") && (literal <= 127));
            return (negativeMax || positiveMax) && this.varHasReg((Operand) leftOperand, varTable, varTable.get(dest.getName()).getVirtualReg());
        }

        return false;
    }

    private boolean varHasReg(Operand operand, HashMap<String, Descriptor> varTable, int reg) {
        Descriptor varDescriptor = varTable.get(operand.getName());
        if(varDescriptor == null) {
            return false;
        }

        return varDescriptor.getVirtualReg() == reg;
    }

    private int getIincIntegerValue(AssignInstruction assign) {
        int result = 0;

        TreeNode leftOperand = assign.getRhs().getChildren().get(0);
        TreeNode rightOperand = assign.getRhs().getChildren().get(1);

        OperationType opType = ((BinaryOpInstruction) assign.getRhs()).getOperation().getOpType();
        if(leftOperand.getClass() == LiteralElement.class) {
            result = Integer.parseInt(((LiteralElement) leftOperand).getLiteral());
        } else if(rightOperand.getClass() == LiteralElement.class) {
            result = Integer.parseInt(((LiteralElement) rightOperand).getLiteral());
        }

        if(opType.name().equals("SUB") && rightOperand.getClass() == LiteralElement.class) {
            result *= -1;
        }

        return result;
    }

    private void generateIincAssign(AssignInstruction assign, StringBuilder code) {
        code.append("iinc ").append(this.currentMethod.getVarTable().get(((Operand) assign.getDest()).getName()).getVirtualReg()).append(" ").append(this.getIincIntegerValue(assign)).append(NL);
    }

    private  void generateNormalAssign(AssignInstruction assign, StringBuilder code) {
        var operand = (Operand) assign.getDest();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        code.append(
                switch (operand.getType().toString()) {
                    case "INT32", "INT", "BOOLEAN" -> "istore";
                    default -> "astore";
                }
        ).append(reg < 4 ? "_" : " ").append(reg).append(NL);
    }

    private void generateArrayAssign(AssignInstruction assign, StringBuilder code) {
        var lhs = (ArrayOperand) assign.getDest();
        var reg = currentMethod.getVarTable().get(lhs.getName()).getVirtualReg();
        code.append("aload").append(reg < 4 ? "_" : " ").append(reg).append(NL);
        code.append(generators.apply(lhs.getIndexOperands().get(0)));

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        code.append("iastore").append(NL);
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        StringBuilder generatedResult = new StringBuilder();

        int literalValue = Integer.valueOf(literal.getLiteral());
        if(this.constantValueExistsInConstantPool(literalValue)) {
            generatedResult.append("iconst_" + literalValue);
        } else if(this.isByte(literalValue)){
            generatedResult.append("bipush " + literalValue);
        } else if(this.isShort(literalValue)){
            generatedResult.append("sipush " + literalValue);
        } else {
            generatedResult.append("ldc " + literalValue);
        }

        this.increaseLimitStack();

        return generatedResult.toString() + NL;
    }

    private boolean isShort(int value) {
        return (value <= 32767) && (value >= -32768);
    }

    private boolean isByte(int value) {
        return (value <= 127) && (value >= -128);
    }

    private boolean constantValueExistsInConstantPool(int value) {
        return (value >= this.minConstantPoolValue) && (value <= this.maxConstantPoolValue);
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        this.increaseLimitStack();

        if (!(operand instanceof ArrayOperand)) {
            return switch (operand.getType().toString()) {
                case "INT32", "INT", "BOOLEAN" -> "iload";
                default -> "aload";
            } + (reg < 4 ? "_" : " ") + reg + NL;
        }

        return "aload" + (reg < 4 ? "_" : " ") + reg + NL +
                generators.apply(((ArrayOperand) operand).getIndexOperands().get(0)) +
                "iaload" + NL;
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
            case LTH -> this.lthCode(binaryOp);
            case ANDB -> this.andbCode(binaryOp);
            case GTE -> this.gteCode(binaryOp);
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        this.decreaseLimitStack();

        return code.toString();
    }

    private String gteCode(BinaryOpInstruction binaryOp) {
        StringBuilder code = new StringBuilder();

        code.append("isub").append(NL);
        this.decreaseLimitStack();

        int labelValue = this.currentLthLabel;

        String trueLabel = this.getCmpTrueLabel(labelValue);
        String endLabel = this.getCmpEndLabel(labelValue);

        code.append("ifge ").append(trueLabel).append(NL);
        code.append("iconst_0").append(NL);
        code.append("goto ").append(endLabel).append(NL);

        // 1. Generate cmp_0__true_label
        code.append(trueLabel).append(":").append(NL);
        code.append("iconst_1").append(NL);

        this.increaseLimitStack(); // We just increment once besides having two iconst instructions because they are mutually exclusive

        code.append(endLabel).append(":").append(NL);

        this.currentLthLabel++;

        return code.toString();    }

    private String andbCode(BinaryOpInstruction binaryOp) {
        StringBuilder code = new StringBuilder();

        // 1. Avaliar left
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append("ifne ").append(this.getCmpTrueLabel(this.currentLthLabel)).append(NL);

        // 2. Se left for falso, sair logo e dar push do 0
        code.append("iconst_0").append(NL);
        code.append("goto ").append(this.getCmpEndLabel(this.currentLthLabel)).append(NL);

        // 3. Este é o bloco de código onde vamos verificar o right
        code.append(this.getCmpTrueLabel(this.currentLthLabel)).append(":").append(NL);
        code.append(generators.apply(binaryOp.getRightOperand()));
        code.append("ifne ").append(this.getCmpTrueLabel(this.currentLthLabel + 1)).append(NL);
        code.append("iconst_0").append(NL);
        code.append("goto ").append(this.getCmpEndLabel(this.currentLthLabel)).append(NL);

        // 4. Dar push de verdadeiro caso o right seja diferente de zero
        code.append(this.getCmpTrueLabel(this.currentLthLabel + 1)).append(":").append(NL);
        code.append("iconst_1").append(NL);

        // 5. Código final
        code.append(this.getCmpEndLabel(this.currentLthLabel)).append(":").append(NL);

        this.currentLthLabel += 2;

        return code.toString();
    }

    private String lthCode(BinaryOpInstruction binaryOp) {
        StringBuilder code = new StringBuilder();

        code.append("isub").append(NL);
        this.decreaseLimitStack();

        int labelValue = this.currentLthLabel;

        String trueLabel = this.getCmpTrueLabel(labelValue);
        String endLabel = this.getCmpEndLabel(labelValue);

        code.append("iflt ").append(trueLabel).append(NL);
        code.append("iconst_0").append(NL);
        code.append("goto ").append(endLabel).append(NL);

        // 1. Generate cmp_0__true_label
        code.append(trueLabel).append(":").append(NL);
        code.append("iconst_1").append(NL);

        this.increaseLimitStack(); // We just increment once besides having two iconst instructions because they are mutually exclusive

        code.append(endLabel).append(":").append(NL);

        this.currentLthLabel++;

        return code.toString();
    }

    private String getCmpEndLabel(int labelValue) {
        return "cmp_" + labelValue + "_end_label";
    }

    private String getCmpTrueLabel(int labelValue) {
        return "cmp_" + labelValue + "_true_label";
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        boolean voidReturningMethod = false;
        if(returnInst.getOperand() == null) {
            voidReturningMethod = true;
        }

        if(!voidReturningMethod) {
            code.append(generators.apply(returnInst.getOperand()));
            Type returnType = returnInst.getReturnType();
            if(returnType.getTypeOfElement().name().equals("INT32") || returnType.getTypeOfElement().name().equals("BOOLEAN")) {
                code.append("ireturn").append(NL);
            } else {
                code.append("areturn").append(NL);
            }
        } else if(voidReturningMethod) {
            code.append("return");
        }

        return code.toString();
    }

    private String generateCall(CallInstruction callInst) {
        StringBuilder code = new StringBuilder();

        CallType invocationType = callInst.getInvocationType();

        switch (invocationType) {
            case invokestatic -> generateInvokeStatic(callInst, code);
            case invokevirtual -> generateInvokeVirtual(callInst, code);
            case NEW -> {
                if (callInst.getOperands().size() == 1) {
                    generateNewClass(callInst, code);
                } else {
                    generateNewArray(callInst, code);
                }
            }
            case invokespecial -> generateSpecial(callInst, code);
            case arraylength -> generateArrayLength(callInst, code);
            default -> throw new NotImplementedException("Invocation type " + invocationType.name() + " not implemented in CallInstruction");
        }

        return code.toString();
    }

    private void generateArrayLength(CallInstruction callInst, StringBuilder code) {
        code.append(generators.apply(callInst.getCaller()));
        code.append("arraylength").append(NL);
    }

    private void generateInvokeStatic(CallInstruction callInst, StringBuilder code) {
        String argumentsType = pushArgumentsIntoStackAndGetType(callInst, code);

        code.append(callInst.getInvocationType().toString());
        code.append(" ");
        code.append(JasminMethodUtils.importFullPath(callInst.getOperands().get(0).toString().split(": ")[1].split("\\.")[0], this.classUnitImports));
        code.append("/");

        Pattern pattern = Pattern.compile("\"(.*)\"");
        Matcher matcher = pattern.matcher(callInst.getMethodName().toString());

        if (matcher.find())
            code.append(matcher.group(1));

        code.append("(");
        code.append(argumentsType);
        code.append(")");

        code.append(JasminMethodUtils.getTypeInJasminFormat(callInst.getReturnType(), this.classUnitImports));
        code.append(NL);
    }

    private void generateInvokeVirtual(CallInstruction callInst, StringBuilder code) {

        //push caller na stack
        code.append(generators.apply(callInst.getOperands().get(0)));

        //push arguments na stack
        String argumentTypes = pushArgumentsIntoStackAndGetType(callInst, code);

        code.append(callInst.getInvocationType().toString());
        code.append(" ");
        code.append(JasminMethodUtils.getTypeInJasminFormatMethodParam(callInst.getCaller().getType(), this.classUnitImports));
        code.append("/");

        Pattern pattern = Pattern.compile("\"(.*)\"");
        Matcher matcher = pattern.matcher(callInst.getMethodName().toString());

        if (matcher.find())
            code.append(matcher.group(1));

        code.append("(");
        code.append(argumentTypes);
        code.append(")");

        code.append(JasminMethodUtils.getTypeInJasminFormat(callInst.getReturnType(), this.classUnitImports));
        if(!callInst.getReturnType().getTypeOfElement().name().equals("VOID") && !this.currentCallInstructionIsOnAssign) {
            code.append(NL);
            code.append("pop");
            this.decreaseLimitStack();
        }

        if(this.currentCallInstructionIsOnAssign) {
            this.currentCallInstructionIsOnAssign = false;
        }

        code.append(NL);
    }

    private void generateNewClass(CallInstruction callInst, StringBuilder code) {
        code.append("new ");
        this.increaseLimitStack();
        code.append(JasminMethodUtils.importFullPath(callInst.getOperands().get(0).toString().split(": ")[1].split("\\.")[0], this.classUnitImports));
        code.append(NL);
        code.append("dup");
        this.increaseLimitStack();
        code.append(NL);
    }

    private void generateNewArray(CallInstruction callInst, StringBuilder code) {
        code.append(generators.apply(callInst.getOperands().get(callInst.getOperands().size()-1)));
        code.append("newarray int").append(NL);
        this.increaseLimitStack();
    }

    private void generateSpecial(CallInstruction callInst, StringBuilder code) {
        code.append(generators.apply(callInst.getOperands().get(0)));
        code.append("invokespecial ");
        code.append(JasminMethodUtils.getTypeInJasminFormatMethodParam(callInst.getOperands().get(0).getType(), this.classUnitImports));
        code.append("/");
        code.append("<init>()V");
        code.append(NL);
        code.append("pop");
        this.decreaseLimitStack();
        code.append(NL);
    }

    // da push dos arguments na stack e retorna uma string com os tipos dos argumentos
    private String pushArgumentsIntoStackAndGetType(CallInstruction inst, StringBuilder code) {
        StringBuilder argumentsType = new StringBuilder();

        if (inst.getArguments().size() > 0) {
            inst.getArguments().forEach(argument -> {
                code.append(generators.apply(argument));
                argumentsType.append(JasminMethodUtils.getTypeInJasminFormat(argument.getType(), this.classUnitImports));
            });
        }

        return argumentsType.toString();
    }


    private String generatePutField(PutFieldInstruction putFieldInstruction) {
        StringBuilder code = new StringBuilder();

        code.append("aload_0").append(NL);
        this.increaseLimitStack();

        // This will also increase the stack, since it will apply the generators to the children
        code.append(generators.apply(putFieldInstruction.getOperands().get(2)));

        code.append("putfield");
        code.append(" ");
        code.append(ollirResult.getOllirClass().getClassName());
        code.append("/");
        code.append(putFieldInstruction.getField().getName());
        code.append(" ");
        code.append(JasminMethodUtils.getTypeInJasminFormat(putFieldInstruction.getValue().getType(), this.classUnitImports));
        code.append(NL);

        this.decreaseLimitStack();

        return code.toString();
    }

    private void increaseLimitStack() {
        this.currentStackValue += 1;
        this.stack = Math.max(this.currentStackValue, this.stack);
    }

    private void decreaseLimitStack() {
        this.stack = Math.max(this.stack, this.currentStackValue);
        this.currentStackValue--;
    }

    private String generateGetField(GetFieldInstruction getFieldInstruction) {
        StringBuilder code = new StringBuilder();

        code.append("aload_0").append(NL);
        this.increaseLimitStack();

        code.append("getfield");
        code.append(" ");
        code.append(ollirResult.getOllirClass().getClassName());
        code.append("/");
        code.append(getFieldInstruction.getField().getName());
        code.append(" ");
        code.append(JasminMethodUtils.getTypeInJasminFormat(getFieldInstruction.getField().getType(), this.classUnitImports));
        code.append(NL);

        this.increaseLimitStack();

        return code.toString();
    }
}
