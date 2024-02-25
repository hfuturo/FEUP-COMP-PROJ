package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pt.up.fe.comp2024.ast.Kind.METHOD_DECL;
import static pt.up.fe.comp2024.ast.Kind.VAR_DECL;

public class JmmSymbolTableBuilder {


    public static JmmSymbolTable build(JmmNode root) {

        var classDecl = root.getJmmChild(0);
        System.out.println(classDecl.getKind());
        System.out.println(Kind.CLASS_DECL.getNodeName());
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");

        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);

        return new JmmSymbolTable(className, methods, returnTypes, params, locals);
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), new Type(TypeUtils.getIntTypeName(), false)));

        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), Arrays.asList(new Symbol(intType, method.getJmmChild(1).get("name")))));

        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        map.put(classDecl.get("name"), classDecl.getChildren(VAR_DECL).stream()
                .map(JmmSymbolTableBuilder::getSymbolBasedOnType)
                .toList());

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {

        return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }


    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        return methodDecl.getChildren(VAR_DECL).stream()
                .map(JmmSymbolTableBuilder::getSymbolBasedOnType)
                .toList();
    }

    private static Symbol getSymbolBasedOnType(JmmNode varDecl) {
        JmmNode type_node = varDecl.getJmmChild(0);
        Type type;
        switch (type_node.getKind()) {
            case "IntegerType":
                type = new Type(TypeUtils.getIntTypeName(), false);
                break;
            case "BoolType":
                type = new Type(TypeUtils.getBoolTypeName(), false);
                break;
            case "ArrayType":
                type = new Type(TypeUtils.getIntTypeName(), true);
                break;
            case "AbstractDataType":
                type = new Type(type_node.get("name"), false);
                break;
            default:
                type = new Type("unknown", false);
                break;
        }

        return new Symbol(type, varDecl.get("name"));
    }
}
