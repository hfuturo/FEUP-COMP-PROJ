package pt.up.fe.comp.cp2;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class SemanticAnalysisTest {

    @Test
    public void symbolTable() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/SymbolTable.jmm"));
        System.out.println("Symbol Table:\n" + result.getSymbolTable().print());
    }

    @Test
    public void varNotDeclared() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/VarNotDeclared.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void classNotImported() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ClassNotImported.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void intPlusObject() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/IntPlusObject.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void boolTimesInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/BoolTimesInt.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void intMethodTimesInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/intMethodTimesInt.jmm"));
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void intArrayAccessTimesInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/intArrayAccessTimesInt.jmm"));
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void boolLessThanInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/BoolLessThanInt.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayPlusInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ArrayPlusInt.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayAccessOnInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ArrayAccessOnInt.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayIndexNotInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ArrayIndexNotInt.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void assignIntToBool() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/AssignIntToBool.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void objectAssignmentFail() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ObjectAssignmentFail.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void objectAssignmentPassExtends() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ObjectAssignmentPassExtends.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void objectAssignmentPassImports() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ObjectAssignmentPassImports.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void intInIfCondition() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/IntInIfCondition.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void boolInIfCondition() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/BoolInIfCondition.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void arrayInWhileCondition() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ArrayInWhileCondition.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void boolInWhileCondition() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/BoolInWhileCondition.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void callToUndeclaredMethod() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/CallToUndeclaredMethod.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void callToMethodAssumedInExtends() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/CallToMethodAssumedInExtends.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void callToMethodAssumedInImport() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/CallToMethodAssumedInImport.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void incompatibleArguments() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/IncompatibleArguments.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void incompatibleReturn() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/IncompatibleReturn.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void assumeArguments() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/AssumeArguments.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void varargs() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/Varargs.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void varargsWrong() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/VarargsWrong.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void varargsDuplicate() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/VarargsDuplicate.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayInit() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ArrayInit.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void arrayInitWrong1() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ArrayInitWrong1.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayInitWrong2() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ArrayInitWrong2.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void thisInStaticMethod() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ThisInStaticMethod.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void assignThisToVar() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/AssignThisToVar.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void assignThisToVarError() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/AssignThisToVarError.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void assignLength() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/AssignLength.jmm"));
        TestUtils.noErrors(result);
    }

  /*  @Test
    public void testCorrectAssignScopes() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/TestCorrectAssignScopes.jmm"));
        TestUtils.noErrors(result);
    } */

    @Test
    public void checkVarInsideMain() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/CheckVarInsideMain.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void callUndeclaredMethodInNewInstance() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/CallUndeclaredMethodInNewInstance.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void callDeclaredMethodInNewInstance() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/CallDeclaredMethodInNewInstance.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void callImportMethodPresentInClass() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/CallImportMethodPresentInClass.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void assignToStaticClass() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/AssignToStaticClass.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void returnStaticClass() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ReturnStaticClass.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void duplicatedImports() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/DuplicatedImports.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void duplicatedMethods() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/DuplicatedMethods.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void duplicatedMainMethod() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/DuplicatedMainMethod.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void declareMethodWithNameMain() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/DeclareMethodWithNameMain.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void duplicatedVars() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/DuplicatedVars.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void duplicatedMainMethodVars() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/DuplicatedMainMethodVars.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void duplicatedFields() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/DuplicatedFields.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void duplicatedParams() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/DuplicatedParams.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void varDeclaredInParam() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/VarDeclaredInParam.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void miscMainEverywhereOk() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/MiscMainEverywhereOk.jmm"));
        System.out.println(result.getReports());
    }
/*
    @Test
    public void miscLengthAsNameOk() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/MiscLengthAsNameOk.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void fieldInStaticInvalid() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/FieldInStaticInvalid.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    } */

    @Test
    public void varArgWrongArrayType() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/VarArgArrayWrongType.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void newClassNotImportedError() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/NewClassNotImportedError.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void testInheritedClassInReturnAndArguments() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/TestInheritedClassInReturnAndArguments.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void assignSuperToClassError() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/AssignSuperToClassError.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void returnSuperToClassError() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/ReturnSuperToClassError.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void accessValidArray() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/AccessValidArray.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void methodChainInvocationOk() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/MethodChainInvocationOk.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void methodChainInvocationError() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/semanticanalysis/MethodChainInvocationError.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }
}
