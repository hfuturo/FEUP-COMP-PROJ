package pt.up.fe.comp2024.analysis;

public class SemanticErrorUtils {
    public static String undeclaredVarMsg(String varName) {
        return String.format("Variable '%s' does not exist.", varName);
    }
}
