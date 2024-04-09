package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Type;

public class JasminMethodUtils {
    public static String getTypeInJasminFormat(Type type) {
        return switch (type.toString()) {
            case "VOID" -> "V";
            case "INT" -> "I";
            case "BOOLEAN" -> "Z";
            default -> "";
        };
    }
}
