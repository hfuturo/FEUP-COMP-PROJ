package pt.up.fe.comp2024.optimization;


import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;

public class OptUtils {
    private static int tempNumber = -1;

    public static String getTemp() {

        return getTemp("tmp");
    }

    public static String getTemp(String prefix) {

        return prefix + getNextTempNum();
    }

    public static int getNextTempNum() {

        tempNumber += 1;
        return tempNumber;
    }

    public static String toOllirType(JmmNode typeNode) {
        String typeName = typeNode.get("name");
        if (typeNode.isInstance(Kind.ARRAY_TYPE) || typeNode.isInstance(Kind.VAR_ARG_TYPE)) {
            return ".array" + toOllirType(typeName);
        }

        return toOllirType(typeName);
    }

    public static String toOllirType(Type type) {
        return toOllirType(type.getName(), type.isArray());
    }

    //necessário verificar se TYPE é IMPORT para atualizar!!!
    private static String toOllirType(String typeName) {
        String type = "." + switch (typeName) {
            case "int", "<" -> "i32";
            case "boolean", "&&", "!" -> "bool";
            default -> typeName; // main class ou import
        };

        return type;
    }

    private static String toOllirType(String typeName, boolean isArray) {
        String type = "." + switch (typeName) {
            case "int", "<" -> "i32";
            case "boolean", "&&", "!" -> "bool";
            default -> typeName; // main class ou import
        };

        return isArray ? (".array" + type) : type;
    }


}
