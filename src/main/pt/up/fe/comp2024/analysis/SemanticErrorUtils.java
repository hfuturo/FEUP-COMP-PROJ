package pt.up.fe.comp2024.analysis;

public class SemanticErrorUtils {
    public static String undeclaredVarMsg(String varName) {
        return String.format("Variable '%s' does not exist.", varName);
    }

    public static String incompatibleType(String leftType, String rightType, String operator) {
        return String.format("Incompatible types (%s, %s) in operation: %s", leftType, rightType, operator);
    }

    public static String incompatibleType(String type, String operator) {
        return String.format("Incompatible type (%s) in operation: %s", type, operator);
    }

    public static String lengthOnSomethingNotArray(String name) {
        return String.format("You cannot call length on a variable (%s) which is not an array", name);
    }

    // type é import, metodo, var, ...
    public static String alreadyExistsError(String type, String name) {
        return String.format("(%s) with name (%s) already exists", type, name);
    }
}
