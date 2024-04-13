package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.Type;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JasminMethodUtils {
    public static String getTypeInJasminFormat(Type type) {
        String typeString = type.toString();

        return switch (typeString) {
            case "VOID" -> "V";
            case "INT" -> "I";
            case "INT32" -> "I";
            case "STRING[]" -> "[Ljava.lang.String;";
            case "STRING" -> "Ljava.lang.String";
            case "BOOLEAN" -> "Z";
            default -> {
                Pattern pattern = Pattern.compile("\\((.*)\\)");
                Matcher matcher = pattern.matcher(typeString);

                // CLASS(x) e THIS(x)
                if (matcher.find()) {
                    yield matcher.group(1);
                }

                throw new RuntimeException(String.format("Type %s has no specified conversion to jasmin format", type.toString()));
            }
        };
    }
}
