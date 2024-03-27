package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.*;

public class JmmSymbolTableBuilder {


    public static JmmSymbolTable build(JmmNode root) {
        var classDecl = root.getJmmChild(0);
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");

        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);
        var imports = buildImports(classDecl);
        var superSymbol = buildSuperSymbol(classDecl);

        return new JmmSymbolTable(className, methods, returnTypes, params, locals, imports, superSymbol);
    }

    private static List<String> buildImports(JmmNode classDecl) {
        List<String> imports = new ArrayList<>();

        classDecl.getChildren("Import").forEach(imp -> {
            var import_path =  imp.getObjectAsList("names", String.class);
            imports.add(import_path.get(import_path.size()-1));
        });

        return imports;
    }
    private static String buildSuperSymbol(JmmNode classDecl) {
        Optional<String> result = classDecl.getOptional("ultraSuper");

        return result.orElse("");
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();

        if (!classDecl.getChildren(MAIN_METHOD).isEmpty()) {
            map.put("main", new Type("void", false));
        }

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getType(method.getChild(0))));

        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        var mainMethod = classDecl.getChildren(MAIN_METHOD);
        if (!mainMethod.isEmpty()) {
            var c = mainMethod.get(0).getChildren(INNER_MAIN_METHOD);
            map.put("main", new ArrayList<>(List.of(new Symbol(new Type(TypeUtils.getStringTypeName(), true), c.get(0).get("name")))));
        }

        classDecl.getChildren(MAIN_METHOD)
                .forEach(method -> method.getChildren(PARAM)
                        .forEach(param -> {
                            map.put("main", new ArrayList<>(List.of(getSymbolBasedOnType((param)))));
                        }));

        classDecl.getChildren(METHOD_DECL)
                .forEach(method -> {
                        map.put(method.get("name"), new ArrayList<>());
                        method.getChildren(PARAM)
                            .forEach(param -> map.get(method.get("name")).add(getSymbolBasedOnType(param)));
                });
        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        map.put(classDecl.get("name"), classDecl.getChildren(VAR_DECL).stream()
                .map(JmmSymbolTableBuilder::getSymbolBasedOnType)
                .toList());

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        classDecl.getChildren(MAIN_METHOD).stream()
                .forEach(method -> method.getChildren(INNER_MAIN_METHOD).stream()
                        .forEach(main_method -> map.put("main", getLocalsList(main_method))));

        System.out.println("Map is: " + map);

        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {
        List<String> ret = new ArrayList<>();

        if (!classDecl.getChildren(MAIN_METHOD).isEmpty()) {
            ret.add("main");
        }

        ret.addAll(classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList());
        return ret;
    }


    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        System.out.println("Children: " + methodDecl.getChildren(VAR_DECL));
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

    private static Type getType(JmmNode node) {
        return switch (node.getKind()) {
            case "IntegerType" -> new Type(TypeUtils.getIntTypeName(), false);
            case "BoolType" -> new Type(TypeUtils.getBoolTypeName(), false);
            case "ArrayType" -> new Type(TypeUtils.getIntTypeName(), true);
            case "StringType" -> new Type(TypeUtils.getStringTypeName(), false);
            case "AbstractDataType" -> new Type(node.get("name"), false);
            default -> new Type("unknown", false);
        };
    }
}
