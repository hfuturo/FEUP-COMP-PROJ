package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.Type;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JasminMethodUtils {
    public static String importFullPath(String name, List<String> imports) {
        for(String importName: imports) {
            String[] importComponents = importName.split("\\.");

            for(int i = 0; i < importComponents.length; i++) {
                String imp = importComponents[i];

                if(imp.equals(name)) {
                    return importName.replace('.', '/');
                }
            }
        }

        return name;
    }
    public static String getTypeInJasminFormatMethodParam(Type type, List<String> imports) {
        String typeString = type.toString();

        if(TypeUtils.abstractType(typeString)) {
            Pattern pattern = Pattern.compile("\\((.*)\\)");
            Matcher matcher = pattern.matcher(typeString);

            // CLASS(x) e THIS(x)
            if (matcher.find()) {
                String s = matcher.group(1);
                if(TypeUtils.abstractType(s)) {
                    return String.format("L%s;", JasminMethodUtils.importFullPath(s, imports));
                }
            }
        }

        return JasminMethodUtils.getTypeInJasminFormat(type);
    }

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
