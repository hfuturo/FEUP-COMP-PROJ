package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Type;

public class JasminMethodUtils {
    public static String getTypeInJasminFormat(Type type) {
        return switch (type.toString()) {
            case "VOID" -> "V";
            case "INT" -> "I";
            case "INT32" -> "I";
            case "STRING[]" -> "java.lang.String[]";
            case "BOOLEAN" -> "Z";
            default -> throw new RuntimeException(String.format("Type %s has no specified conversion to jasmin format", type.toString()));
        };
    }
}
