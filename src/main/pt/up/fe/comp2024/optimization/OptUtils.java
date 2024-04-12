package pt.up.fe.comp2024.optimization;


import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

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
        //Kind.fromString(typeNode.getKind()).checkIsTypeOrThrow(typeNode);

        String typeName = typeNode.get("name");

        return toOllirType(typeName);
    }

    public static String toOllirType(Type type) {
        return toOllirType(type.getName());
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


}
