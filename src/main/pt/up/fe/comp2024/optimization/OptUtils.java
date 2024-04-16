package pt.up.fe.comp2024.optimization;


import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.TypeUtils;

public class OptUtils {
    private static final String OLLIR_BOOL_NAME = "BOOLEAN";
    private static final String OLLIR_INT_NAME = "INT32";
    private static final String OLLIR_STRING_NAME = "STRING";
    private static final String OLLIR_VOID_NAME = "VOID";
    private static int tempNumber = -1;

    public static boolean ollirAbstractType(String typeName) {
        return !(typeName.equals(OLLIR_BOOL_NAME) || typeName.equals(OLLIR_INT_NAME) || typeName.equals(OLLIR_VOID_NAME) || typeName.equals(OLLIR_STRING_NAME));
    }

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
